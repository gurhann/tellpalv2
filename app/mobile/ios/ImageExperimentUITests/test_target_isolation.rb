# Read-only membership verification. It neither provisions nor builds an application.
require 'xcodeproj'
require 'rexml/document'

ios = File.expand_path('..', __dir__)
project = Xcodeproj::Project.open(File.join(ios, 'Runner.xcodeproj'))
runner = project.targets.find { |target| target.name == 'Runner' }
app = project.targets.find { |target| target.name == 'ImageExperiment' }
tests = project.targets.find { |target| target.name == 'ImageExperimentUITests' }
raise 'TARGET_ISOLATION_REFUSED' unless runner && app && tests
raise 'TARGET_ISOLATION_REFUSED' unless project.build_configurations.all? { |config| config.build_settings['SWIFT_VERSION'] == '5.0' }
raise 'TARGET_ISOLATION_REFUSED' if runner.source_build_phase.files_references.any? { |file| file.real_path.to_s.include?('/ImageExperiment/') }
raise 'TARGET_ISOLATION_REFUSED' unless app.source_build_phase.files_references.map(&:path).sort ==
  %w[AppDelegate.swift GeneratedPluginRegistrant.m ImageExperimentBootstrap.swift ImageExperimentLedger.swift SceneDelegate.swift].sort
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
puts 'XCODE_TARGET_ISOLATION_PASS'
