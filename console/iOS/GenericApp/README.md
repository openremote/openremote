#  <#Title#>

WizardDomainViewController
-> user enters URL or domain
  -> if only domain, URL is https://<domain>.openremote.app
  -> GET <base URL>/api/master/apps/consoleConfig
    -> see https://github.com/openremote/openremote/issues/642
    -> if 404, use defaults
    -> if 200, read info
       showAppTextInput: Boolean -> default false
       showRealmTextInput: Boolean -> default false 
       app: String? -> default nil
       allowedApps: List<String>? -> default nil/empty
       apps: Map<String, ORAppInfo>? -> default nil/empty

    -> if allowedApps empty -> GET <base URL>/api/master/apps
      -> if 200, e.g. ["console_loader","manager"]
      -> if non 200 -> ???


In the end, we want to have a config with all information:
- URL
- app
- realm
and maybe a name

We can pass this object around in the different wizard screens + information each screen need to render
We can store this object as the configuration


https://test1.openremote.app/<app>/info.json 
