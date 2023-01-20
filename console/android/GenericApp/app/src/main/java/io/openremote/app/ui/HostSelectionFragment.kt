package io.openremote.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.fragment.app.Fragment
import io.openremote.app.R
import io.openremote.app.databinding.FragmentHostSelectionBinding
import io.openremote.app.model.ORAppInfo
import io.openremote.app.model.ORConsoleConfig
import io.openremote.app.network.ApiManager


/**
 * A simple [Fragment] subclass.
 * Use the [HostSelectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HostSelectionFragment : Fragment() {
    private lateinit var binding: FragmentHostSelectionBinding
    private lateinit var parentActivity: ProjectWizardActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        parentActivity = requireActivity() as ProjectWizardActivity

        // Inflate the layout for this fragment
        binding = FragmentHostSelectionBinding.inflate(inflater, container, false);
        val view: View = binding.root


        binding.connectButton.setOnClickListener {
            if (!binding.hostInput.text.isNullOrBlank()) {
                connectToHost(binding.hostInput.text.toString())
            }
            parentActivity.host = binding.hostInput.text.toString()
        }

        binding.backButton.setOnClickListener {
            activity?.finish()
        }

        return view
    }

    private fun connectToHost(host: String) {
        parentActivity.binding.progressBar.visibility = View.VISIBLE
        val url =
            if (URLUtil.isValidUrl(host)) host.plus("/api/master") else "https://${host}.openremote.app/api/master"
        parentActivity.apiManager = ApiManager(url)
        parentActivity.apiManager.getConsoleConfig { statusCode, consoleConfig, error ->
            when (statusCode) {
                in 200..299 -> {
                    consoleConfig?.let { config ->
                        if (config.app != null) {
                            parentActivity.app = config.app
                            val appInfo = config.apps?.get(config.app)
                            if (appInfo != null) {
                                parentActivity.binding.progressBar.visibility = View.INVISIBLE
                                processAppInfo(config, appInfo)
                            } else {
                                parentActivity.apiManager.getAppInfos { statusCode, appInfoMap, error ->
                                    parentActivity.binding.progressBar.visibility = View.INVISIBLE
                                    when (statusCode) {
                                        in 200..299 -> {
                                            if (appInfoMap != null) {
                                                val appInfo = appInfoMap[config.app!!]
                                                if (appInfo != null) {
                                                    processAppInfo(config, appInfo)
                                                } else {
                                                    parentFragmentManager.beginTransaction()
                                                        .replace(
                                                            R.id.fragmentContainer,
                                                            AppSelectionFragment.newInstance(
                                                                appList = config.allowedApps,
                                                                appMap = config.apps,
                                                                showAppTextInput = config.showAppTextInput,
                                                                showRealmTextInput = config.showRealmTextInput
                                                            )
                                                        )
                                                        .addToBackStack(ProjectWizardActivity.TAG)
                                                        .commit()
                                                }
                                            } else {
                                                parentFragmentManager.beginTransaction()
                                                    .replace(
                                                        R.id.fragmentContainer,
                                                        AppSelectionFragment.newInstance(
                                                            appList = config.allowedApps,
                                                            appMap = config.apps,
                                                            showAppTextInput = config.showAppTextInput,
                                                            showRealmTextInput = config.showRealmTextInput
                                                        )
                                                    )
                                                    .addToBackStack(ProjectWizardActivity.TAG)
                                                    .commit()
                                            }
                                        }
                                        404 -> { //no appconfig
                                            parentActivity.goToMainActivity(
                                                parentActivity.app!!,
                                                "master"
                                            )
                                        }
                                        else -> {
                                            binding.errorView.visibility = View.VISIBLE
                                            parentActivity.runOnUiThread {
                                                binding.errorView.visibility = View.VISIBLE
                                                binding.errorTextView.text =
                                                    resources.getText(R.string.error_getting_app_info)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            parentActivity.binding.progressBar.visibility = View.INVISIBLE
                            parentFragmentManager.beginTransaction()
                                .replace(
                                    R.id.fragmentContainer,
                                    AppSelectionFragment.newInstance(
                                        appList = config.allowedApps,
                                        appMap = config.apps,
                                        showAppTextInput = config.showAppTextInput,
                                        showRealmTextInput = config.showRealmTextInput
                                    )
                                )
                                .addToBackStack(ProjectWizardActivity.TAG)
                                .commit()
                        }
                    }
                }
                404 -> { //no console_config.json
                    parentActivity.apiManager.getAppInfos { statusCode, appInfoMap, error ->
                        parentActivity.binding.progressBar.visibility = View.INVISIBLE
                        when (statusCode) {
                            in 200..299 -> {
                                if (appInfoMap?.isNotEmpty() == true) {
                                    parentFragmentManager.beginTransaction()
                                        .replace(
                                            R.id.fragmentContainer,
                                            AppSelectionFragment.newInstance(
                                                appList = null,
                                                appMap = appInfoMap
                                            )
                                        )
                                        .addToBackStack(ProjectWizardActivity.TAG)
                                        .commit()
                                } else {
                                    parentActivity.apiManager.getApps { statusCode, apps, error ->
                                        if (apps?.isNotEmpty() == true) {
                                            parentFragmentManager.beginTransaction()
                                                .replace(
                                                    R.id.fragmentContainer,
                                                    AppSelectionFragment.newInstance(
                                                        appList = apps,
                                                        appMap = null
                                                    )
                                                )
                                                .addToBackStack(ProjectWizardActivity.TAG)
                                                .commit()
                                        } else {
                                            //TODO remove inserting manager app after all manager instances are updated with console_settings_improvement branch
                                            val tempApps = arrayListOf("manager")
                                            parentFragmentManager.beginTransaction()
                                                .replace(
                                                    R.id.fragmentContainer,
                                                    AppSelectionFragment.newInstance(
                                                        appList = tempApps,
                                                        appMap = null,
                                                        showAppTextInput = true
                                                    )
                                                )
                                                .addToBackStack(ProjectWizardActivity.TAG)
                                                .commit()

//                                            parentActivity.runOnUiThread {
//                                                binding.errorView.visibility = View.VISIBLE
//                                                binding.errorTextView.text =
//                                                    getString(R.string.no_apps_found)
//                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                parentActivity.apiManager.getApps { statusCode, apps, error ->
                                    if (apps?.isNotEmpty() == true) {
                                        parentFragmentManager.beginTransaction()
                                            .replace(
                                                R.id.fragmentContainer,
                                                AppSelectionFragment.newInstance(
                                                    appList = apps,
                                                    appMap = null,
                                                    showAppTextInput = true
                                                )
                                            )
                                            .addToBackStack(ProjectWizardActivity.TAG)
                                            .commit()
                                    } else {
                                        //TODO remove inserting manager app after all manager instances are updated with console_settings_improvement branch
                                        val tempApps = arrayListOf("manager")
                                        parentFragmentManager.beginTransaction()
                                            .replace(
                                                R.id.fragmentContainer,
                                                AppSelectionFragment.newInstance(
                                                    appList = tempApps,
                                                    appMap = null
                                                )
                                            )
                                            .addToBackStack(ProjectWizardActivity.TAG)
                                            .commit()
//                                        parentActivity.runOnUiThread {
//                                            binding.errorView.visibility = View.VISIBLE
//                                            binding.errorTextView.text =
//                                                getString(R.string.no_apps_found)
//                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    parentActivity.binding.progressBar.visibility = View.INVISIBLE
                    parentActivity.runOnUiThread {
                        binding.errorView.visibility = View.VISIBLE
                        binding.errorTextView.text =
                            resources.getText(R.string.could_not_connect_to_domain)
                    }
                }
            }
        }
    }

    private fun processAppInfo(config: ORConsoleConfig, appInfo: ORAppInfo) {
        parentActivity.consoleProviders = appInfo.providers
        if (appInfo.consoleAppIncompatible) {
            parentActivity.runOnUiThread {
                binding.errorView.visibility = View.VISIBLE
                binding.errorTextView.text = getString(R.string.app_incompatible)
            }
        } else if (parentActivity.app != null) {
            parentActivity.goToMainActivity(
                parentActivity.app!!,
                consoleProviders = appInfo.providers
            )
        } else {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    RealmSelectionFragment.newInstance(
                        realmList = appInfo.realms,
                        showRealmTextInput = config.showRealmTextInput
                    )
                )
                .addToBackStack(ProjectWizardActivity.TAG)
                .commit()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HostSelectionFragment()
    }
}
