Pod::Spec.new do |s|
  s.platform = :ios
  s.ios.deployment_target = '10'
  s.pod_target_xcconfig = {
    "OTHER_LDFLAGS" => '$(inherited) -framework "FirebaseCore" -framework "FirebaseMessaging"',
    "CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES" => 'YES',
    "FRAMEWORK_SEARCH_PATHS" => '$(inherited) "${PODS_ROOT}/FirebaseCore/Frameworks" "${PODS_ROOT}/FirebaseMessaging/Frameworks"'
  }
  s.name = "ORLib"
  s.summary = "The OpenRemote library to create console apps."
  s.requires_arc = true
  s.version = '1.0.0'
  s.authors = 'OpenRemote'
  s.homepage = "http://www.openremote.com/community/"
  s.source = { :git => 'https://github.com/openremote/openremote.git', :tag => s.version.to_s }
  s.dependency 'Firebase/Core', '4.6.0'
  s.dependency 'Firebase/Messaging', '4.6.0'

  base_dir = "console/iOS/"
  s.source_files = base_dir + 'ORLib/*.{swift}'
  s.license = { :type => "MIT", :file => base_dir + "LICENSE" }
end
