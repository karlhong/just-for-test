package com.trendmicro.tmmssuite.service;

import com.trendmicro.android.base.util.Log;

import com.trendmicro.tmmssuite.framework.ui.VersionInfo;
import com.trendmicro.tmmssuite.util.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tracy_miao on 2017/3/31.
 */

public class UpgradeAppRequest extends UniAPI
{
    public static final String TAG = ServiceConfig.makeLogTag(UpgradeAppRequest.class);

    public UpgradeAppRequest(Boolean isRetryWhenPossible, String jobID)
    {
        super(isRetryWhenPossible, !isRetryWhenPossible, true,
                ServiceConfig.JOB_START_UPGRADE_APP_REQUEST_INTENT,
                ServiceConfig.JOB_START_UPGRADE_APP_REQUEST_SUCC_INTENT,
                ServiceConfig.JOB_START_UPGRADE_APP_REQUEST_ERRO_INTENT,
                ServiceConfig.HTTP_UNI_URL + "UpgradeApp", jobID);
    }

    @Override
    protected String genRequestString() throws JSONException, ServiceErrorException
    {
        String authKey = getAuthKey(false);
        String accountID = getAccountId(false);
        if (authKey.equals("") || accountID.equals(""))
        {
            Log.e(TAG, "No authKey or accountID to register with account!");
            throw new ServiceErrorException(ServiceConfig.ERROR_PARAMETER_JSON_ENCODING_ERROR);
        }
        Map<String, String> data = new HashMap<String, String>();
        data.put("AuthKey", authKey);
        data.put("AccountID", accountID);
        data.put("PID", getPid(true));
        data.put("VID", getVid(true));
        data.put("UniqueID", serviceDelegate.prefHelper.uid());
        data.put("Locale", serviceDelegate.prefHelper.locale());
        data.put("Model", serviceDelegate.prefHelper.model());
        data.put("APPVER", VersionInfo.getFullVerString());
        data.put("IAPAccount", getGoogleAccount());
        data.put("DeviceCountryIso", Utils.getCountryCodeFromSimCard(serviceDelegate.getApplicationContext()));
        data.put("ProjectCode", getProjectCode());
        String requestString = ProtocolJsonParser.genRequest(UpgradeAppRequest.class, data);
        Log.d(TAG, "UpgradeAppRequest request is " + requestString);
        return requestString;
    }

    @Override
    protected String processResponseBody(String responseBody) throws JSONException, ResponseDecodingException, ServiceErrorException, IOException
    {
        ProtocolJsonParser.UpgradeAppResponse parsedResponse = ProtocolJsonParser.parseResponse(ProtocolJsonParser.UpgradeAppResponse.class, responseBody);
        String responseCode = parsedResponse.responseCode;
        if (responseCode.equals("0"))
        {
            storeConsumerAccountId(parsedResponse.ConsumerAccountID);

            if (!serviceDelegate.jobStore.isCancelJob(jobID))
            {
                HTTPPostJob nextJob = new UpdateDeviceInfoRequest(isRetryWhenPossible, jobID);
                nextJob.onErrorIntentAction = this.onErrorIntentAction;
                nextJob.onSuccessIntentAction = this.onSuccessIntentAction;
                nextJob.serviceDelegate = this.serviceDelegate;
                nextJob.internalExecute();
            }
            else
            {
                serviceDelegate.jobStore.deleteJob(jobID);
            }
        }
        else
        {
            Log.e(TAG, "Upgrade App Request error! " + responseCode + " " + parsedResponse.responseError);
            int intResponseCode = Integer.parseInt(responseCode);
            if (intResponseCode != ServiceConfig.ERROR_INVALID_AUTHKEY)
            {
                throw new ServiceErrorException(intResponseCode);
            }
        }
        return responseCode;
    }
}
