#import "GeofireFlutterPlugin.h"
#if __has_include(<geofire_flutter/geofire_flutter-Swift.h>)
#import <geofire_flutter/geofire_flutter-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "geofire_flutter-Swift.h"
#endif

@implementation GeofireFlutterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftGeofireFlutterPlugin registerWithRegistrar:registrar];
}
@end
