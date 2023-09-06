package com.carusto.ReactNativePjSip.action;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.carusto.ReactNativePjSip.PjActionType;
import com.carusto.ReactNativePjSip.PjSipAccount;
import com.carusto.ReactNativePjSip.PjSipService;
import com.carusto.ReactNativePjSip.dto.AccountConfigurationDTO;
import com.facebook.react.bridge.ReadableMap;
import org.pjsip.pjsua2.*;

import java.util.Map;

public class AccountCreateAction extends PjSipReactAction implements PjSipActionIntentHandler {

    private static final String TAG = "CreateAccountAction";


    public static Intent createIntent(int callbackId, ReadableMap configuration, Context context) {
        Intent intent = createCallbackIntent(PjActionType.ACTION_CREATE_ACCOUNT, callbackId, context);
        formatIntent(intent, configuration);
        return intent;
    }


    @Override
    public void handle(PjSipService service, Intent intent) {
        try {
            AccountConfigurationDTO accountConfiguration = AccountConfigurationDTO.fromIntent(intent);
            PjSipAccount account = doAccountCreate(service, accountConfiguration);


            super.sendEventHandledJson(service, intent, account.toJson());
            // Emmit response
        } catch (Exception e) {
            super.sendEventException(service, intent, e);
        }
    }

    private PjSipAccount doAccountCreate( PjSipService service, AccountConfigurationDTO configuration) throws Exception {
        AccountConfig cfg = new AccountConfig();

        // General settings
        AuthCredInfo cred = new AuthCredInfo(
            "Digest",
            configuration.getNomalizedRegServer(),
            configuration.getUsername(),
            0,
            configuration.getPassword()
        );

        String idUri = configuration.getIdUri();
        String regUri = configuration.getRegUri();


        cfg.getNatConfig().setSipStunUse(pjsua_stun_use.PJSUA_STUN_RETRY_ON_FAILURE);
        cfg.getNatConfig().setMediaStunUse(pjsua_stun_use.PJSUA_STUN_RETRY_ON_FAILURE);
        //if (.isIce()) {
        //cfg.getNatConfig().setIceEnabled(true);
        //cfg.getNatConfig().setIceAlwaysUpdate(true);
        //cfg.getNatConfig().setIceAggressiveNomination(true);

//        cfg.getNatConfig().setSipStunUse(pjsua_stun_use.PJSUA_STUN_RETRY_ON_FAILURE);
//        cfg.getNatConfig().setMediaStunUse(pjsua_stun_use.PJSUA_STUN_RETRY_ON_FAILURE);
//        cfg.getNatConfig().setIceEnabled(false);
        //cfg.getNatConfig().setIceTrickle(pj_ice_sess_trickle.PJ_ICE_SESS_TRICKLE_HALF);
        //cfg.getNatConfig().setUdpKaIntervalSec(1);

        cfg.setIdUri(idUri);
        cfg.getRegConfig().setRegistrarUri(regUri);
        cfg.getRegConfig().setRegisterOnAdd(configuration.isRegOnAdd());
        cfg.getSipConfig().getAuthCreds().add(cred);

        cfg.getVideoConfig().getRateControlBandwidth();

        // Registration settings

        if (configuration.getContactParams() != null) {
            cfg.getSipConfig().setContactParams(configuration.getContactParams());
        }
        if (configuration.getContactUriParams() != null) {
            cfg.getSipConfig().setContactUriParams(configuration.getContactUriParams());
        }
        if (configuration.getRegContactParams() != null) {
            Log.w(TAG, "Property regContactParams are not supported on android, use contactParams instead");
        }

        if (configuration.getRegHeaders() != null && configuration.getRegHeaders().size() > 0) {
            SipHeaderVector headers = new SipHeaderVector();

            for (Map.Entry<String, String> entry : configuration.getRegHeaders().entrySet()) {
                SipHeader hdr = new SipHeader();
                hdr.setHName(entry.getKey());
                hdr.setHValue(entry.getValue());
                headers.add(hdr);
            }

            cfg.getRegConfig().setHeaders(headers);
        }

        Map<String, Integer> transportIds = service.getTransportIds();
        // Transport settings
        Integer transportId = transportIds.get("TCP");

        if (configuration.isTransportNotEmpty()) {
            if (transportIds.containsKey(configuration.getTransport())) {
                transportId = transportIds.get(configuration.getTransport());
            } else {
                Log.w(TAG, "Illegal \"" + configuration.getTransport() + "\" transport (possible values are UDP, TCP or TLS) use TCP instead");
            }
        }
        // Fallback to some transport... This most likely will fail
        if (transportId == null ) {
            transportId = transportIds.get(0);
        }

        cfg.getSipConfig().setTransportId(transportId);

        if (configuration.isProxyNotEmpty()) {
            StringVector v = new StringVector();
            v.add(configuration.getProxy());
            cfg.getSipConfig().setProxies(v);
        }

        {
            SrtpOpt opt = new SrtpOpt();
            IntVector optVector = new IntVector();
            optVector.add(pjmedia_srtp_keying_method.PJMEDIA_SRTP_KEYING_DTLS_SRTP);
            optVector.add(pjmedia_srtp_keying_method.PJMEDIA_SRTP_KEYING_SDES);
            opt.setKeyings(optVector);
            cfg.getMediaConfig().setSrtpOpt(opt);
        }

        cfg.getMediaConfig().getTransportConfig().setQosType(pj_qos_type.PJ_QOS_TYPE_VOICE);
        cfg.getMediaConfig().setSrtpUse(pjmedia_srtp_use.PJMEDIA_SRTP_OPTIONAL);
        cfg.getMediaConfig().getTransportConfig().setPort(20000);
        cfg.getMediaConfig().getTransportConfig().setPortRange(65000);

        //cfg.getMediaConfig().setSrtpSecureSignaling(1);
        //cfg.getMediaConfig().setRtcpMuxEnabled(true);

        {
            //cfg.getMediaConfig().getTransportConfig().getTlsConfig().setVerifyServer(false);
            //cfg.getMediaConfig().getTransportConfig().getTlsConfig().setVerifyClient(false);
            //cfg.getMediaConfig().getTransportConfig().getTlsConfig().setCertBuf();
            //cfg.getMediaConfig().getTransportConfig().getTlsConfig().setMethod(pjsip_ssl_method.PJSIP_TLSV1_2_METHOD);
        }
        cfg.getVideoConfig().setAutoShowIncoming(true);
        cfg.getVideoConfig().setAutoTransmitOutgoing(true);

        int cap_dev = cfg.getVideoConfig().getDefaultCaptureDevice();
        service.getEndpoint().vidDevManager().setCaptureOrient(cap_dev, pjmedia_orient.PJMEDIA_ORIENT_ROTATE_270DEG, true);

        // -----

        PjSipAccount account = new PjSipAccount(service, transportId, configuration);
        account.create(cfg);

        service.addTrash(cfg);
        service.addTrash(cred);

        service.addAccount(account);

        return account;
    }

}
