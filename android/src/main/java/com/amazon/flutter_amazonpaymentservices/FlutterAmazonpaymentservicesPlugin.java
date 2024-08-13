package com.amazon.flutter_amazonpaymentservices;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import android.app.Activity;
import android.content.Intent;

import com.payfort.fortpaymentsdk.FortSdk;
import com.payfort.fortpaymentsdk.callbacks.FortCallBackManager;
import com.payfort.fortpaymentsdk.callbacks.FortCallback;
import com.payfort.fortpaymentsdk.callbacks.FortInterfaces;
import com.payfort.fortpaymentsdk.callbacks.PayFortCallback;
import com.payfort.fortpaymentsdk.domain.model.FortRequest;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;

public class FlutterAmazonpaymentservicesPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    private static final String METHOD_CHANNEL_KEY = "flutter_amazonpaymentservices";
    private static final int PAYFORT_REQUEST_CODE = 1166;
    private static FortCallBackManager fortCallback;
    private MethodChannel methodChannel;
    private static Activity activity;
    private Constants.ENVIRONMENTS_VALUES mEnvironment;
    private Result pendingResult;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel = new MethodChannel(binding.getBinaryMessenger(), METHOD_CHANNEL_KEY);
        methodChannel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        this.pendingResult = result; // Set the pending result

        switch (call.method) {
            case "normalPay":
                handleOpenFullScreenPayfort(call);
                break;
            case "getUDID":
                result.success(FortSdk.getDeviceId(activity));
                break;
            case "validateApi":
                handleValidateAPI(call);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener((requestCode, resultCode, data) -> {
            if (requestCode == PAYFORT_REQUEST_CODE) {
                if (data != null) {
                    if (resultCode == RESULT_OK) {
                        fortCallback.onActivityResult(requestCode, resultCode, data);
                    } else {
                        Intent intent = new Intent();
                        fortCallback.onActivityResult(requestCode, resultCode, intent);
                    }
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        methodChannel.setMethodCallHandler(null);
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener((requestCode, resultCode, data) -> {
            if (requestCode == PAYFORT_REQUEST_CODE) {
                if (data != null) {
                    if (resultCode == RESULT_OK) {
                        fortCallback.onActivityResult(requestCode, resultCode, data);
                    } else {
                        Intent intent = new Intent();
                        fortCallback.onActivityResult(requestCode, resultCode, intent);
                    }
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    private void handleValidateAPI(MethodCall call) {
        if (call.argument("environmentType").toString().equals("production")) {
            mEnvironment = Constants.ENVIRONMENTS_VALUES.PRODUCTION;
        } else {
            mEnvironment = Constants.ENVIRONMENTS_VALUES.SANDBOX;
        }

        HashMap<String, Object> requestParamMap = call.argument("requestParam");
        FortRequest fortRequest = new FortRequest();
        fortRequest.setRequestMap(requestParamMap);

        FortSdk.getInstance().validate(activity, FortSdk.ENVIRONMENT.TEST, fortRequest, new PayFortCallback() {
            @Override
            public void startLoading() {
                // No-op
            }

            @Override
            public void onSuccess(@NonNull Map<String, ?> fortResponseMap, @NonNull Map<String, ?> map1) {
                if (pendingResult != null) {
                    pendingResult.success(fortResponseMap);
                    pendingResult = null; // Clear the pending result after use
                }
            }

            @Override
            public void onFailure(@NonNull Map<String, ?> fortResponseMap, @NonNull Map<String, ?> map1) {
                if (pendingResult != null) {
                    pendingResult.error("onFailure", "onFailure", fortResponseMap);
                    pendingResult = null; // Clear the pending result after use
                }
            }
        });
    }

    private void handleOpenFullScreenPayfort(MethodCall call) {
        try {
            if (call.argument("environmentType").toString().equals("production")) {
                mEnvironment = Constants.ENVIRONMENTS_VALUES.PRODUCTION;
            } else {
                mEnvironment = Constants.ENVIRONMENTS_VALUES.SANDBOX;
            }

            boolean isShowResponsePage = call.argument("isShowResponsePage");
            HashMap<String, Object> requestParamMap = call.argument("requestParam");
            FortRequest fortRequest = new FortRequest();
            fortRequest.setShowResponsePage(isShowResponsePage);
            fortRequest.setRequestMap(requestParamMap);

            if (fortCallback == null) {
                fortCallback = FortCallback.Factory.create();
            }

            FortSdk.getInstance().registerCallback(activity, fortRequest, mEnvironment.getSdkEnvironemt(), PAYFORT_REQUEST_CODE, fortCallback, true, new FortInterfaces.OnTnxProcessed() {
                @Override
                public void onCancel(Map<String, Object> requestParamsMap, Map<String, Object> responseMap) {
                    if (pendingResult != null) {
                        pendingResult.error("onCancel", "onCancel", responseMap);
                        pendingResult = null; // Clear the pending result after use
                    }
                }

                @Override
                public void onSuccess(Map<String, Object> requestParamsMap, Map<String, Object> fortResponseMap) {
                    if (pendingResult != null) {
                        pendingResult.success(fortResponseMap);
                        pendingResult = null; // Clear the pending result after use
                    }
                }

                @Override
                public void onFailure(Map<String, Object> requestParamsMap, Map<String, Object> fortResponseMap) {
                    if (pendingResult != null) {
                        pendingResult.error("onFailure", "onFailure", fortResponseMap);
                        pendingResult = null; // Clear the pending result after use
                    }
                }
            });
        } catch (Exception e) {
            HashMap<Object, Object> errorDetails = new HashMap<>();
            errorDetails.put("response_message", e.getMessage());
            if (pendingResult != null) {
                pendingResult.error("onFailure", "onFailure", errorDetails);
                pendingResult = null; // Clear the pending result after use
            }
        }
    }
}