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
  s.version = '0.1.0'
  s.license = { :type => "MIT", :file => "LICENSE" }
  s.authors = 'OpenRemote'
  s.homepage = "http://www.openremote.com/community/"
  s.source = { :git => 'https://bitbucket.org/AppBriek/openremoteios.git', :tag => s.version.to_s }
  s.dependency 'Firebase/Core', '4.6.0'
  s.dependency 'Firebase/Messaging', '4.6.0'
  s.source_files = 'ORLib/**/*.{swift}'
  s.resources = 'ORLib/**/*.{storyboard,xib}'
end
