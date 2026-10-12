# Read-only membership verification. It neither provisions nor builds an application.
require 'xcodeproj'
require 'rexml/document'

def verify_native_pods(project_path)
  pods = Xcodeproj::Project.open(project_path)
  target = pods.targets.find { |item| item.name == 'Pods-ImageExperimentUITests' }
  raise 'TARGET_ISOLATION_REFUSED' unless target && target.dependencies.empty?
  raise 'TARGET_ISOLATION_REFUSED' unless target.source_build_phase.files_references.map(&:path) ==
    ['Pods-ImageExperimentUITests-dummy.m']
  raise 'TARGET_ISOLATION_REFUSED' unless target.frameworks_build_phase.files_references.length == 1 &&
    target.frameworks_build_phase.files_references.all? { |file|
    (file.source_tree == 'SDKROOT' && file.path == 'System/Library/Frameworks/Foundation.framework') ||
      (file.source_tree == 'DEVELOPER_DIR' && file.path.match?(%r{\APlatforms/iPhoneOS\.platform/Developer/SDKs/iPhoneOS\d+(?:\.\d+)*\.sdk/System/Library/Frameworks/Foundation\.framework\z}))
  }
  raise 'TARGET_ISOLATION_REFUSED' unless target.build_configurations.all? { |config|
    config.build_settings['MACH_O_TYPE'] == 'staticlib' &&
      ['', nil].include?(config.build_settings['OTHER_LDFLAGS']) &&
      config.base_configuration_reference &&
      File.read(config.base_configuration_reference.real_path).lines.none? { |line|
        line.match?(/^\s*(OTHER_LDFLAGS|FRAMEWORK_SEARCH_PATHS|HEADER_SEARCH_PATHS|LIBRARY_SEARCH_PATHS)\s*=/)
      }
  }
end

if ARGV.length == 2 && ARGV[0] == '--pods-only'
  verify_native_pods(ARGV[1])
  puts 'XCODE_TARGET_ISOLATION_PASS'
  exit
end
raise 'TARGET_ISOLATION_REFUSED' unless ARGV.empty? || (ARGV.length == 2 && ARGV[0] == '--ios')
ios = ARGV.empty? ? File.expand_path('..', __dir__) : File.realpath(ARGV[1])
project = Xcodeproj::Project.open(File.join(ios, 'Runner.xcodeproj'))
runner = project.targets.find { |target| target.name == 'Runner' }
app = project.targets.find { |target| target.name == 'ImageExperiment' }
tests = project.targets.find { |target| target.name == 'ImageExperimentUITests' }
raise 'TARGET_ISOLATION_REFUSED' unless runner && app && tests
raise 'TARGET_ISOLATION_REFUSED' unless project.build_configurations.all? { |config| config.build_settings['SWIFT_VERSION'] == '5.0' }
raise 'TARGET_ISOLATION_REFUSED' if runner.source_build_phase.files_references.any? { |file| file.real_path.to_s.include?('/ImageExperiment/') }
raise 'TARGET_ISOLATION_REFUSED' unless app.source_build_phase.files_references.map(&:path).sort ==
  %w[AppDelegate.swift GeneratedPluginRegistrant.m ImageExperimentAuditAppDelegate.swift ImageExperimentBootstrap.swift
     ImageExperimentInstallationAudit.swift ImageExperimentLedger.swift SceneDelegate.swift].sort
raise 'TARGET_ISOLATION_REFUSED' unless tests.source_build_phase.files_references.map(&:path).sort ==
  %w[ImageExperimentBootstrapTests.swift ImageExperimentBootstrap.swift ImageExperimentInstallationAudit.swift
     ImageExperimentLedger.swift].sort
tests.source_build_phase.files_references.each do |file|
  imports = File.read(file.real_path).scan(/^\s*(?:@testable\s+)?import\s+([A-Za-z0-9_]+)/).flatten
  raise 'TARGET_ISOLATION_REFUSED' unless (imports - %w[CryptoKit Darwin Foundation Security UIKit XCTest]).empty?
end
raise 'TARGET_ISOLATION_REFUSED' unless tests.dependencies.map(&:target) == [app]
raise 'TARGET_ISOLATION_REFUSED' unless runner.build_configurations.all? { |config| config.build_settings['PRODUCT_BUNDLE_IDENTIFIER'] == 'com.tellpal.tellpalMobilePoc' }
raise 'TARGET_ISOLATION_REFUSED' unless app.build_configurations.map(&:name).sort ==
  %w[Debug-imageexperiment Profile-imageexperiment Release-imageexperiment].sort
%w[profile release].each do |mode|
  config = app.build_configurations.find { |item| item.name == "#{mode.capitalize}-imageexperiment" }
  raise 'TARGET_ISOLATION_REFUSED' unless config.build_settings['PRODUCT_BUNDLE_IDENTIFIER'] == "com.tellpal.tellpalMobilePoc.imageexperiment.#{mode}"
  text = File.read(config.base_configuration_reference.real_path)
  raise 'TARGET_ISOLATION_REFUSED' if text.include?('Generated.xcconfig')
  raise 'TARGET_ISOLATION_REFUSED' unless text.include?('FLUTTER_TARGET=lib/main_image_experiment.dart')
end
scheme = REXML::Document.new(File.read(File.join(ios, 'Runner.xcodeproj/xcshareddata/xcschemes/imageexperiment.xcscheme')))
%w[TestAction LaunchAction].each do |name|
  action = scheme.elements["Scheme/#{name}"]
  raise 'TARGET_ISOLATION_REFUSED' unless action.attributes['selectedDebuggerIdentifier'] == ''
end
podfile = File.read(File.join(ios, 'Podfile'))
native_pod_block = podfile.match(/target 'ImageExperimentUITests' do(.*?)\n  end/m)
raise 'TARGET_ISOLATION_REFUSED' unless native_pod_block && native_pod_block[1].match?(/inherit!\s*:none/)
raise 'TARGET_ISOLATION_REFUSED' if native_pod_block[1].match?(/\bpod\s|flutter_install_all_ios_pods|use_frameworks!/)
pods_project = File.join(ios, 'Pods/Pods.xcodeproj')
verify_native_pods(pods_project) if File.directory?(pods_project)
puts 'XCODE_TARGET_ISOLATION_PASS'
