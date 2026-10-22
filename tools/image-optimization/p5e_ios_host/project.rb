# Mevcut xcodeproj gem'iyle yalnız P5e host'u oluşturulur; plugin veya pod yoktur.
require 'xcodeproj'
project = Xcodeproj::Project.new('P5eOffline.xcodeproj')
target = project.new_target(:application, 'P5eOffline', :ios, '15.0')
target.add_file_references(['AppDelegate.swift', 'P5eSession.swift'].map { |name| project.main_group.new_file(name) })
config = project.main_group.new_file('P5e.xcconfig')
target.build_configurations.each do |item|
  item.base_configuration_reference = config
  item.build_settings.merge!({
    'PRODUCT_BUNDLE_IDENTIFIER' => 'com.tellpal.tellpalMobilePoc.imageexperiment.release',
    'INFOPLIST_FILE' => 'Info.plist', 'SWIFT_VERSION' => '5.0', 'TARGETED_DEVICE_FAMILY' => '1',
    'ENABLE_BITCODE' => 'NO', 'CODE_SIGN_STYLE' => 'Manual', 'CODE_SIGN_ENTITLEMENTS' => 'P5e.entitlements',
    'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO', 'CLANG_ENABLE_MODULES' => 'YES',
    'FLUTTER_BUILD_MODE' => 'release', 'LD_RUNPATH_SEARCH_PATHS' => '$(inherited) @executable_path/Frameworks',
    'FRAMEWORK_SEARCH_PATHS' => '$(inherited) $(PROJECT_DIR)/Flutter',
    'OTHER_LDFLAGS' => '$(inherited) -framework Flutter',
    'SWIFT_OPTIMIZATION_LEVEL' => '-O', 'SWIFT_COMPILATION_MODE' => 'wholemodule',
  })
end
phase = target.new_shell_script_build_phase('Flutter build')
phase.shell_script = '/bin/sh "$FLUTTER_ROOT/packages/flutter_tools/bin/xcode_backend.sh" build'
phase.always_out_of_date = '1'
target.build_phases.move(phase, 0)
embed = target.new_shell_script_build_phase('Flutter embed')
embed.shell_script = '/bin/sh "$FLUTTER_ROOT/packages/flutter_tools/bin/xcode_backend.sh" embed_and_thin'
embed.always_out_of_date = '1'
project.save
scheme = Xcodeproj::XCScheme.new
scheme.add_build_target(target)
scheme.set_launch_target(target)
scheme.save_as(project.path, 'P5eOffline', true)
