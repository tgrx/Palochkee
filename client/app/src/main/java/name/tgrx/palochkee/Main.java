package name.tgrx.palochkee;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Scroller;
import android.widget.TextView;


public class Main extends ActionBarActivity {
    private TelephonyManager m_telephony;
    private RssiListener m_rssi;
    private Vibrator m_vibrator;
    private LocationManager m_locator;
    private Tracker m_tracker;

    private class RssiListener extends PhoneStateListener {
        public int getRssi() {
            return m_rssi;
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength ss) {
            super.onSignalStrengthsChanged(ss);
            int rssi = ss.getGsmSignalStrength();
            m_rssi = -113 + (2 * (rssi >= 99 ? 0 : rssi));
        }

        private int m_rssi = 0;
    }

    private class Tracker implements LocationListener {
        private Location m_location = null;

        public Location getLocation() {
            return m_location;
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle b) {}

        @Override
        public void onProviderEnabled(String s) {}

        @Override
        public void onProviderDisabled(String s) {}

        @Override
        public void onLocationChanged(Location loc) {
            m_location = loc;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        m_telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        m_vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        m_locator = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        m_rssi = new RssiListener();
        m_telephony.listen(m_rssi, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        m_tracker = new Tracker();
        m_locator.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 1, m_tracker);
        m_locator.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 1, m_tracker);

        TextView cell_info = (TextView) findViewById(R.id.cell_info);
        cell_info.setScroller(new Scroller(getApplicationContext()));
        cell_info.setVerticalScrollBarEnabled(true);
        cell_info.setMovementMethod(new ScrollingMovementMethod());
    }

    public void onGetCellInfoClick(View view) {
        m_vibrator.vibrate(10);
        UpdateCellInfo(view);
        m_vibrator.vibrate(30);
    }

    private void UpdateCellInfo(View view) {
        TextView cell_info = (TextView) findViewById(R.id.cell_info);
        ProgressBar progress = (ProgressBar) findViewById(R.id.progressBar);
        StringBuilder data = new StringBuilder();

        progress.setProgress(0);

        final Criteria bestCriteria = new Criteria();
        bestCriteria.setAltitudeRequired(true);
        bestCriteria.setHorizontalAccuracy(Criteria.ACCURACY_FINE);
        bestCriteria.setVerticalAccuracy(Criteria.ACCURACY_FINE);
        final String bestProvider = m_locator.getBestProvider(bestCriteria, true);

        data.append("[coordinates]\n");
        try {
            final Location coords = m_locator.getLastKnownLocation(bestProvider);
            if (coords != null) {
                data.append("    accuracy: ").append(coords.getAccuracy()).append("\n")
                    .append("    LAT: ").append(coords.getLatitude()).append("\n")
                    .append("    LNG: ").append(coords.getLongitude()).append("\n")
                    .append("    ALT: ").append(coords.getAltitude()).append("\n");
                progress.setProgress(1);

                Geocoder geo = new Geocoder(getApplicationContext());
                data.append("    Addresses:\n");
                try {
                    List<Address> addresses = geo.getFromLocation(coords.getLatitude(), coords.getLongitude(), 2);
                    if (addresses != null && !addresses.isEmpty()) {
                        for (final Address addr: addresses) {
                            data.append("        ")
                                    .append(addr.getAddressLine(0)).append(" ")
                                    .append(addr.getAddressLine(1)).append(" ")
                                    .append(addr.getAddressLine(2)).append("\n");
                        }
                    }
                } catch (IOException e) {
                    data.append("");
                }
                progress.setProgress(2);
            } else {
                data.append("    no data\n");
            }
        } catch (SecurityException e) {
            data.append("    no permission to get location\n");
        } catch (IllegalArgumentException e) {
            data.append("    no data from provider ").append(bestProvider).append("\n");
        }

        data.append("\n");

        data.append("[Network]\n")
                .append("    Operator: ")
                .append(m_telephony.getNetworkOperator())
                .append(" ")
                .append(m_telephony.getNetworkOperatorName())
                .append(" ")
                .append(m_telephony.getNetworkCountryIso())
                .append("\n");
        progress.setProgress(3);

        data.append("    Phone: ");
        switch (m_telephony.getPhoneType()) {
            case TelephonyManager.PHONE_TYPE_GSM:
                data.append("GSM");
                break;
            case TelephonyManager.PHONE_TYPE_SIP:
                data.append("SIP");
                break;
            case TelephonyManager.PHONE_TYPE_CDMA:
                data.append("CDMA");
                break;
            case TelephonyManager.PHONE_TYPE_NONE:
                data.append("NONE");
                break;
        }
        data.append("\n");
        progress.setProgress(4);

        data.append("    Network: ");
        boolean is_umts = false;
        switch (m_telephony.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                data.append("GPRS");
                break;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                data.append("EDGE");
                break;
            case TelephonyManager.NETWORK_TYPE_HSPA:
                data.append("HSPA");
                is_umts = true;
                break;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                data.append("HDSPA");
                is_umts = true;
                break;
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                data.append("HSPA+");
                is_umts = true;
                break;
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                data.append("HSUPA");
                is_umts = true;
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                data.append("3G");
                is_umts = true;
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
                data.append("LTE");
                break;
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default:
                data.append("unknown");
                break;
        }
        data.append("\n");
        progress.setProgress(5);

        data.append("    Data: ");
        switch (m_telephony.getDataActivity()) {
            case TelephonyManager.DATA_ACTIVITY_DORMANT:
                data.append("dormant");
                break;
            case TelephonyManager.DATA_ACTIVITY_IN:
                data.append("in");
                break;
            case TelephonyManager.DATA_ACTIVITY_INOUT:
                data.append("in/out");
                break;
            case TelephonyManager.DATA_ACTIVITY_OUT:
                data.append("out");
                break;
            case TelephonyManager.DATA_ACTIVITY_NONE:
            default:
                data.append("none");
                break;
        }
        data.append("\n");
        progress.setProgress(6);

        data.append("    Current cell:\n");
        data.append("        RSSI: ").append(m_rssi.getRssi()).append(" dBm\n");
        final GsmCellLocation currentCell = (GsmCellLocation) m_telephony.getCellLocation();
        if (currentCell != null) {
            data.append("        CID:  ").append(currentCell.getCid() == -1 ? "" : currentCell.getCid() & 0xFFFF).append("\n")
                .append("        LAC:  ").append(currentCell.getLac() == -1 ? "" : currentCell.getLac() & 0xFFFF).append("\n")
                .append("        PSC:  ").append(currentCell.getPsc() == -1 ? "" : currentCell.getPsc() & 0x1FF).append("\n");
        }
        progress.setProgress(7);

        data.append("\n[cells]\n");
        List<NeighboringCellInfo> neighborCells = m_telephony.getNeighboringCellInfo();
        if (neighborCells != null && !neighborCells.isEmpty()) {
            Collections.sort(neighborCells, new Comparator<NeighboringCellInfo>() {
                public int compare(NeighboringCellInfo first, NeighboringCellInfo second) {
                    int s1 = first.getRssi();
                    int s2 = second.getRssi();
                    return (s1 < s2) ? 1 : ((s1 == s2) ? 0 : -1);
                }
            });
            for (final NeighboringCellInfo info: neighborCells) {
                data.append("    [cell]\n");
                data.append("        LAC/CID: ")
                        .append(info.getLac() == -1 ? "" : info.getLac() & 0xFFFF)
                        .append(" ")
                        .append(info.getCid() == -1 ? "" : info.getCid() & 0xFFFF)
                        .append("\n");
                data.append("        PSC:     ").append(info.getPsc() == -1 ? "" : info.getPsc() & 0x1FF).append("\n");

                data.append("        RSSI:    ");
                final int rssi = info.getRssi();
                if (rssi < 0) {
                    data.append(rssi);
                } else {
                    if (is_umts) {
                        data.append(-116 + rssi);
                    } else {
                        data.append(-113 + 2 * rssi);
                    }
                }
                data.append(" dBm\n");
            }
        } else {
            data.append("Cannot retrieve info about cells!\n");
        }

        cell_info.setText(data.toString());
        progress.setProgress(8);
    }
}
