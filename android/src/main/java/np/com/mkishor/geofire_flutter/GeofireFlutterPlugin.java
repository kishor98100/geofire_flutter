package np.com.mkishor.geofire_flutter;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoQuery;
import com.google.firebase.database.DatabaseReference;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** GeofireFlutterPlugin */
public class GeofireFlutterPlugin implements FlutterPlugin, MethodCallHandler {

  private MethodChannel channel;
  private EventChannel eventChannel;
  private GeoFire geoFire;
  private DatabaseReference databaseReference;
  private GeoQuery circleQuery;
  private Boolean listening = false;
  private EventChannel.EventSink eventSink;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "geofire_flutter");
    eventChannel = new EventChannel( registrar.getBinaryMessenger(),"geofire_flutter_stream");
    channel.setMethodCallHandler(this);
    eventChannel.setStreamHandler(this);
  }

  @Override
    public void onMethodCall(MethodCall call, final Result result) {

        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("GeoFire.start")) {

            databaseReference = FirebaseDatabase.getInstance().getReference(call.argument("path").toString());
            geoFire = new GeoFire(databaseReference);
            if (geoFire.getDatabaseReference() != null) {
                result.success(true);
            } else
                result.success(false);
        } else if (call.method.equals("setLocation")) {

            geoFire.setLocation(call.argument("id").toString(), new GeoLocation(Double.parseDouble(call.argument("lat").toString()), Double.parseDouble(call.argument("lng").toString())), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {

                    if (error != null) {
                        result.success(false);
                    } else {
                        result.success(true);
                    }

                }
            });


        }else if (call.method.equals("removeLocation")) {

            geoFire.removeLocation(call.argument("id").toString(), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {

                    if (error != null) {
                        result.success(false);
                    } else {
                        result.success(true);
                    }

                }
            });


        }else if (call.method.equals("getLocation")) {
            geoFire.getLocation(call.argument("id").toString(),new LocationCallback() {
                @Override
                public void onLocationResult(String key, GeoLocation location) {
                    HashMap<String ,Object> map=new HashMap<>();
                    if (location != null) {

                        map.put("lat",location.latitude);
                        map.put("lng",location.longitude);
                        map.put("error",null);

                    } else {
                        map.put("error",String.format("There is no location for key %s in GeoFire", key));

                    }

                    result.success(map);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    HashMap<String ,Object> map=new HashMap<>();
                    map.put("error","There was an error getting the GeoFire location: " + databaseError);

                    result.success(map);
                }
            });


        } else if (call.method.equals("queryAtLocation")) {
            if (circleQuery == null) {
                Double lat = Double.parseDouble(call.argument("lat").toString());
                Double lng = Double.parseDouble(call.argument("lng").toString());
                Double radius = Double.parseDouble(call.argument("radius").toString());
                GeoLocation location = new GeoLocation(lat, lng);
                this.circleQuery = geoFire.queryAtLocation(location, radius);
            }//otherwise it is already setup!
            result.success(true);
        } else if (call.method.equals("updateLocation")) {
            Double lat = Double.parseDouble(call.argument("lat").toString());
            Double lng = Double.parseDouble(call.argument("lng").toString());
            Double radius = Double.parseDouble(call.argument("radius").toString());
            GeoLocation location = new GeoLocation(lat, lng);
            this.circleQuery.setCenter(location);
            this.circleQuery.setRadius(radius);
            result.success(true);
        } else {
            result.notImplemented();
        }
    }

@Override
    public void onListen(Object o, final EventChannel.EventSink events) {
        this.eventSink = events;
        if (!listening && circleQuery != null){
            listening = true;
            try {
                this.circleQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                    @Override
                    public void onKeyEntered(String key, GeoLocation location) {
                        Map<String,Object> data = new HashMap<String, Object>();
                        data.put("key", key);
                        data.put("lat", location.latitude);
                        data.put("long", location.longitude);
                        data.put("event", "ENTERED");

                        byte[] jsonData = new JSONObject(data).toString().getBytes();
                        eventSink.success(jsonData);
                    }

                    @Override
                    public void onKeyExited(String key) {
                        Map<String,Object> data = new HashMap<String, Object>();
                        data.put("key", key);
                        data.put("event", "EXITED");

                        byte[] jsonData = new JSONObject(data).toString().getBytes();
                        eventSink.success(jsonData);
                    }

                    @Override
                    public void onKeyMoved(String key, GeoLocation location) {
                    }

                    @Override
                    public void onGeoQueryReady() {
                        System.out.println("All initial data has been loaded and events have been fired!");
                        Map<String,Object> data = new HashMap<String, Object>();
                        data.put("event", "GEOQUERY_READY");

                        byte[] jsonData = new JSONObject(data).toString().getBytes();
                        eventSink.success(jsonData);
                    }

                    @Override
                    public void onGeoQueryError(DatabaseError error) {
                        Map<String,Object> data = new HashMap<String, Object>();
                        data.put("error", error);
                        data.put("event", "ERROR");
                        byte[] jsonData = new JSONObject(data).toString().getBytes();
                        eventSink.success(jsonData);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Map<String,Object> data = new HashMap<String, Object>();
                data.put("error", e);
                data.put("event", "ERROR");
                String jsonData = new JSONObject(data).toString();
                System.out.printf(jsonData);
                eventSink.success(jsonData);
            }
        }
    }

    @Override
    public void onCancel(Object o) {
        this.eventSink = null;
        this.circleQuery.removeAllListeners();
        listening = false;
    }
  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
    eventChannel.setStreamHandler(null);
  }
}
