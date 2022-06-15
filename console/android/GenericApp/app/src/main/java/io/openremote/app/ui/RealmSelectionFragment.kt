package io.openremote.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.openremote.app.R
import io.openremote.app.databinding.FragmentRealmSelectionBinding

private const val ARG_REALM_LIST = "realmList"
private const val ARG_SHOW_REALM_TEXT_INPUT = "showRealmTextInput"


/**
 * A simple [Fragment] subclass.
 * Use the [RealmSelectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RealmSelectionFragment : Fragment() {
    private lateinit var binding: FragmentRealmSelectionBinding
    private lateinit var parentActivity: ProjectWizardActivity

    lateinit var realmArrayAdapter: ArrayAdapter<String>

    private var realmList: List<String>? = null
    private var showRealmTextInput: Boolean = false

    private val mapper = jacksonObjectMapper()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentActivity = requireActivity() as ProjectWizardActivity

        arguments?.let {
            it.getString(ARG_REALM_LIST)?.let {
                realmList = mapper.readValue<List<String>>(it)
            }
            showRealmTextInput = it.getBoolean(ARG_SHOW_REALM_TEXT_INPUT, false)
        }

        if (!realmList.isNullOrEmpty()) {
            if (realmList!!.size == 1) {
                parentActivity.realm = realmList!![0]
                parentActivity.goToMainActivity(
                    parentActivity.app!!,
                    parentActivity.realm,
                    parentActivity.consoleProviders
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentRealmSelectionBinding.inflate(inflater, container, false);
        val view: View = binding.root

        if (showRealmTextInput) {
            binding.realmInputLayout.visibility = View.VISIBLE
            binding.realmInput.doOnTextChanged { text, start, before, count ->
                parentActivity.realm = text.toString()
            }
        } else {
            if (realmList != null) {
                realmArrayAdapter = ArrayAdapter(
                    requireActivity(),
                    android.R.layout.simple_spinner_dropdown_item,
                    realmList!!
                )
                binding.realmSpinner.adapter = realmArrayAdapter
                binding.realmSpinner.visibility = View.VISIBLE

                binding.realmSpinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            parentActivity.realm = realmArrayAdapter.getItem(position)
                        }

                        override fun onNothingSelected(p0: AdapterView<*>?) {
                            parentActivity.realm = null
                        }
                    }
            } else {
                binding.realmInputLayout.visibility = View.VISIBLE
                binding.realmInput.doOnTextChanged { text, start, before, count ->
                    parentActivity.realm = text.toString()
                }
            }
        }

        binding.finishButton.setOnClickListener {
            if (parentActivity.realm != null) {
                parentActivity.goToMainActivity(
                    parentActivity.app!!,
                    parentActivity.realm,
                    parentActivity.consoleProviders
                )
            } else {
                parentActivity.runOnUiThread {
                    binding.errorView.visibility = View.VISIBLE
                    binding.errorTextView.text = getString(R.string.select_realm_first)
                }
            }
        }

        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment RealmSelectionFragment.
         */
        @JvmStatic
        fun newInstance(realmList: List<String>?, showRealmTextInput: Boolean = false) =
            RealmSelectionFragment().apply {
                arguments = Bundle().apply {
                    realmList?.let {
                        putString(ARG_REALM_LIST, mapper.writeValueAsString(it))
                    }
                    putBoolean(ARG_SHOW_REALM_TEXT_INPUT, showRealmTextInput)
                }
            }
    }
}
