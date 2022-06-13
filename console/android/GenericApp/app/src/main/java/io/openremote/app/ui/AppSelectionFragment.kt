package io.openremote.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.openremote.app.R
import io.openremote.app.databinding.FragmentAppSelectionBinding
import io.openremote.app.model.ORAppInfo

private const val ARG_APP_LIST = "appList"
private const val ARG_APP_MAP = "appMap"
private const val ARG_SHOW_APP_TEXT_INPUT = "showAppTextInput"
private const val ARG_SHOW_REALM_TEXT_INPUT = "showRealmTextInput"

/**
 * A simple [Fragment] subclass.
 * Use the [AppSelectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AppSelectionFragment : Fragment() {
    private lateinit var binding: FragmentAppSelectionBinding
    private lateinit var parentActivity: ProjectWizardActivity

    lateinit var appArrayAdapter: ArrayAdapter<String>

    private var appList: List<String>? = null
    private var appMap: Map<String, ORAppInfo>? = null
    private var showAppTextInput: Boolean = false
    private var showRealmTextInput: Boolean = false
    private val mapper = jacksonObjectMapper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            it.getString(ARG_APP_LIST)?.let {
                appList = mapper.readValue<List<String>>(it)
            }
            it.getString(ARG_APP_MAP)?.let {
                appMap = mapper.readValue<Map<String, ORAppInfo>>(it)
            }
            showAppTextInput = it.getBoolean(ARG_SHOW_APP_TEXT_INPUT, false)
            showRealmTextInput = it.getBoolean(ARG_SHOW_REALM_TEXT_INPUT, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        parentActivity = requireActivity() as ProjectWizardActivity

        // Inflate the layout for this fragment
        binding = FragmentAppSelectionBinding.inflate(inflater, container, false);
        val view: View = binding.root

        if (showAppTextInput) {
            binding.appInputLayout.visibility = View.VISIBLE
        } else {
            if (appList != null) {
                appArrayAdapter = ArrayAdapter(
                    requireActivity(),
                    android.R.layout.simple_spinner_dropdown_item,
                    appList!!
                )
                binding.appSpinner.adapter = appArrayAdapter
                binding.appSpinner.visibility = View.VISIBLE
            }

            if (appMap != null) {
                appArrayAdapter = ArrayAdapter(
                    requireActivity(),
                    android.R.layout.simple_spinner_dropdown_item,
                    appMap!!.keys.toList()
                )
                binding.appSpinner.adapter = appArrayAdapter
                binding.appSpinner.visibility = View.VISIBLE
            }

            binding.appSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        parentActivity.app = appArrayAdapter.getItem(position)
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        parentActivity.app = null
                    }
                }
        }

        binding.nextButton.setOnClickListener {
            if (parentActivity.app != null) {
                val appInfo = appMap?.get(parentActivity.app)
                if (appInfo == null) {
                    parentActivity.apiManager.getAppInfos { statusCode, appInfoMap, error ->
                        parentActivity.binding.progressBar.visibility = View.INVISIBLE
                        when (statusCode) {
                            in 200..299 -> {
                                appInfoMap?.let {
                                    val appInfo = it[parentActivity.app]
                                    if (appInfo != null) {
                                        parentFragmentManager.beginTransaction()
                                            .replace(
                                                R.id.fragmentContainer,
                                                RealmSelectionFragment.newInstance(
                                                    realmList = appInfo.realms,
                                                    showRealmTextInput = showRealmTextInput
                                                )
                                            )
                                            .addToBackStack(ProjectWizardActivity.TAG)
                                            .commit()
                                    } else {
                                        parentFragmentManager.beginTransaction()
                                            .replace(
                                                R.id.fragmentContainer,
                                                RealmSelectionFragment.newInstance(
                                                    realmList = null,
                                                    showRealmTextInput = showRealmTextInput
                                                )
                                            )
                                            .addToBackStack(ProjectWizardActivity.TAG)
                                            .commit()
                                    }
                                }
                            }
                            else -> {
                                parentActivity.showToastMessage("Error getting app info")
                            }
                        }
                    }
                } else {
                    parentFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            RealmSelectionFragment.newInstance(
                                realmList = appInfo.realms,
                                showRealmTextInput = showRealmTextInput
                            )
                        )
                        .addToBackStack(ProjectWizardActivity.TAG)
                        .commit()
                }
            } else {
                parentActivity.showToastMessage("Select app first")
            }
        }

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment AppSelectionFragment.
         */
        @JvmStatic
        fun newInstance(
            appList: List<String>?,
            appMap: Map<String, ORAppInfo>?,
            showAppTextInput: Boolean = false,
            showRealmTextInput: Boolean = false
        ) =
            AppSelectionFragment().apply {
                arguments = Bundle().apply {
                    appList?.let {
                        putString(ARG_APP_LIST, mapper.writeValueAsString(it))
                    }
                    appMap?.let {
                        putString(ARG_APP_MAP, mapper.writeValueAsString(it))
                    }
                    putBoolean(ARG_SHOW_REALM_TEXT_INPUT, showRealmTextInput)
                    putBoolean(ARG_SHOW_APP_TEXT_INPUT, showAppTextInput)
                }
            }
    }
}
