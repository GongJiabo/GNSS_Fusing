package  com.whu.gnss.gnsslogger.constellations.satellites;

import com.whu.gnss.gnsslogger.constellations.GpsTime;

import java.util.ArrayList;
import java.util.List;

/**
 * 存放一个历元的数据
 * 2020/03/13
 * 白腾飞
 */
public class EpochMeasurement {

    private List<GpsSatellite> gpsSatelliteList;
    private List<QzssSatellite> qzssSatelliteList;
    private List<BdsSatellite> bdsSatelliteList;
    private List<GalileoSatellite> galileoSatelliteList;
    private List<GlonassSatellite> glonassSatelliteList;

    public EpochMeasurement(GpsTime gpsTime, List<GpsSatellite> gpsSatelliteList, List<QzssSatellite> qzssSatelliteList, List<BdsSatellite> bdsSatelliteList, List<GalileoSatellite> galileoSatelliteList, List<GlonassSatellite> glonassSatelliteList) {
        this.setEpochTime(gpsTime);
        this.setGpsSatelliteList(gpsSatelliteList);
        this.setQzssSatelliteList(qzssSatelliteList);
        this.setBdsSatelliteList(bdsSatelliteList);
        this.setGalileoSatelliteList(galileoSatelliteList);
        this.setGlonassSatelliteList(glonassSatelliteList);
    }

    /**
     * 获取卫星prn号总的列表
     */
    public List<String> getSatellitePrnList() {
        List<String> satellitePrnList=new ArrayList<>();
        if(this.gpsSatelliteList.size()>0)
        {for(GpsSatellite gpsSatellite :gpsSatelliteList)
            satellitePrnList.add(gpsSatellite.getPrn());}
        if(this.galileoSatelliteList.size()>0)
        {for(GalileoSatellite galileoSatellite : galileoSatelliteList)
            satellitePrnList.add(galileoSatellite.getPrn());}
        if(this.qzssSatelliteList.size()>0)
        {for(QzssSatellite qzssSatellite :qzssSatelliteList)
            satellitePrnList.add(qzssSatellite.getPrn());}
        if(this.glonassSatelliteList.size()>0)
        {for(GlonassSatellite glonassSatellite : glonassSatelliteList)
            satellitePrnList.add(glonassSatellite.getPrn());}
        if(this.bdsSatelliteList.size()>0)
        {for(BdsSatellite bdsSatellite : bdsSatelliteList)
            satellitePrnList.add(bdsSatellite.getPrn());}
        return satellitePrnList;
    }

    /**
     * 获取卫星数目，针对于rinex3.03
     */
    private int satelliteNum;

    public int getSatelliteNum() {


        return gpsSatelliteList.size()+qzssSatelliteList.size()+bdsSatelliteList.size()+galileoSatelliteList.size()+glonassSatelliteList.size();
    }
    /**
     * 获取卫星数目，针对于rinex2.11
     */
    private int satelliteNum2;

    public int getSatelliteNum2() {
        return gpsSatelliteList.size()+galileoSatelliteList.size()+glonassSatelliteList.size();
    }
    /**
     * 获取卫星的prn号的列表,针对于rinex2.11
     */
    private List<String> prnlist=new ArrayList<>();

    public List<String> getPrnlist() {
        if(this.gpsSatelliteList.size()>0) {
            for (GpsSatellite gpsSatellite : this.gpsSatelliteList) {
                prnlist.add(gpsSatellite.getPrn());
            }
        }
        if(this.galileoSatelliteList.size()>0) {
            for (GalileoSatellite galileoSatellite : this.galileoSatelliteList) {
                prnlist.add(galileoSatellite.getPrn());
            }
        }
        if(this.glonassSatelliteList.size()>0) {
            for (GlonassSatellite glonassSatellite : this.glonassSatelliteList) {
                prnlist.add(glonassSatellite.getPrn());
            }
        }
        return this.prnlist;
    }



    /**
     * 历元时间
     */
    private GpsTime epochTime;

    private void setEpochTime(GpsTime epochTime) {
        this.epochTime = epochTime;
    }

    public GpsTime getEpochTime() {
        return epochTime;
    }

    /**
     * 存放每个历元下的gps卫星列表数据
     */
    private void setGpsSatelliteList(List<GpsSatellite> gpsSatelliteList) {
        this.gpsSatelliteList = gpsSatelliteList;
    }

    public List<GpsSatellite> getGpsSatelliteList() {
        return gpsSatelliteList;
    }

    /**
     * 存放每个历元下的qzss卫星列表数据
     */
    private void setQzssSatelliteList(List<QzssSatellite> qzssSatelliteList) {
        this.qzssSatelliteList = qzssSatelliteList;
    }

    public List<QzssSatellite> getQzssSatelliteList() {
        return qzssSatelliteList;
    }

    /**
     * 存放每个历元下的bds卫星列表数据
     */
    private void setBdsSatelliteList(List<BdsSatellite> bdsSatelliteList) {
        this.bdsSatelliteList = bdsSatelliteList;
    }

    public List<BdsSatellite> getBdsSatelliteList() {
        return bdsSatelliteList;
    }

    /**
     * 存放每个历元下的galileo卫星列表数据
     */
    private void setGalileoSatelliteList(List<GalileoSatellite> galileoSatelliteList) {
        this.galileoSatelliteList = galileoSatelliteList;
    }

    public List<GalileoSatellite> getGalileoSatelliteList() {
        return galileoSatelliteList;
    }

    /**
     * 存放每个历元下的glonass卫星列表数据
     */


    private void setGlonassSatelliteList(List<GlonassSatellite> glonassSatelliteList) {
        this.glonassSatelliteList = glonassSatelliteList;
    }

    public List<GlonassSatellite> getGlonassSatelliteList() {
        return glonassSatelliteList;
    }

}
