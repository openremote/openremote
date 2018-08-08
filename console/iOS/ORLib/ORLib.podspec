Pod::Spec.new do |s|
  s.platform = :ios
  s.ios.deployment_target = '10'
  s.pod_target_xcconfig = {
    "OTHER_LDFLAGS" => '$(inherited) -framework "FirebaseCore" -framework "FirebaseMessaging"',
    "CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES" => 'YES',
    "FRAMEWORK_SEARCH_PATHS" => '$(inherited) "${PODS_ROOT}/FirebaseCore/Frameworks" "${PODS_ROOT}/FirebaseMessaging/Frameworks"'
  }
  s.swift_version = '4.1'
  s.name = "ORLib"
  s.summary = "The OpenRemote library to create console apps."
  s.requires_arc = true
  s.version = '1.1.1'
  s.authors = 'OpenRemote'
  s.homepage = "http://www.openremote.io"
  s.source = { :git => 'https://github.com/openremote/openremote.git', :tag => 'cocoapod' + s.version.to_s }
  s.dependency 'Firebase/Core', '4.6.0'
  s.dependency 'Firebase/Messaging', '4.6.0'
  s.source_files = 'console/iOS/ORLib/ORLib/*.{swift}', 'console/iOS/ORLib/ORLib/ConsoleProviders/*.{swift}'
  s.license = { :type => "MIT", :file => "console/iOS/ORLib/LICENSE" }
end
