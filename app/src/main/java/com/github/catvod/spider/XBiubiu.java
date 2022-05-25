package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;

import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Misc;
import com.github.catvod.utils.okhttp.OkHttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class XBiubiu extends Spider {

    @Override
    public void init(Context context) {
        super.init(context);
    }

    public void init(Context context, String extend) {
        super.init(context, extend);
        this.ext = extend;
    }

    @Override
    public String homeContent(boolean filter) {
        try {
            fetchRule();
            JSONObject result = new JSONObject();
            JSONArray classes = new JSONArray();
            String[] fenleis = getRuleVal("fenlei", "").split("#");
            for (String fenlei : fenleis) {
                String[] info = fenlei.split("\\$");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type_name", info[0]);
                jsonObject.put("type_id", info[1]);
                classes.put(jsonObject);
            }
            result.put("class", classes);
            return result.toString();
        } catch (
                Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    protected HashMap<String, String> getHeaders(String url) {
        HashMap<String, String> headers = new HashMap<>();
        String ua = getRuleVal("User", Misc.UaWinChrome).trim();
        if (ua.isEmpty())
            ua = Misc.UaWinChrome;
        headers.put("User-Agent", ua);
        return headers;
    }

    @Override
    public String homeVideoContent() {
        try {
            fetchRule();
            if (getRuleVal("shouye").equals("1")) {
                JSONArray videos = new JSONArray();
                String[] fenleis = getRuleVal("fenlei", "").split("#");
                for (String fenlei : fenleis) {
                    String[] info = fenlei.split("\\$");
                    JSONObject data = category(info[1], "1", false, new HashMap<>());
                    if (data != null) {
                        JSONArray vids = data.optJSONArray("list");
                        if (vids != null) {
                            for (int i = 0; i < vids.length() && i < 5; i++) {
                                videos.put(vids.getJSONObject(i));
                            }
                        }
                    }
                    if (videos.length() >= 30)
                        break;
                }
                JSONObject result = new JSONObject();
                result.put("list", videos);
                return result.toString();
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private JSONObject category(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        try {
            fetchRule();
            String webUrl = getRuleVal("url") + tid + pg + getRuleVal("houzhui");
            String html = fetch(webUrl);
            String parseContent = html;
            boolean shifouercijiequ = getRuleVal("shifouercijiequ").equals("1");
            if (shifouercijiequ) {
                String jiequqian = getRuleVal("jiequqian");
                String jiequhou = getRuleVal("jiequhou");
                parseContent = subContent(html, jiequqian, jiequhou, 0);
            }
            String jiequshuzuqian = getRuleVal("jiequshuzuqian");
            String jiequshuzuhou = getRuleVal("jiequshuzuhou");
            resetSubIdx();
            JSONArray videos = new JSONArray();
            String jiequContent = subContent(parseContent, jiequshuzuqian, jiequshuzuhou, 0);
            while (subContentIdx[1] != -1) {
                try {
                    int jiequStartIdx = subContentIdx[1];
                    String title = subContent(jiequContent, getRuleVal("biaotiqian"), getRuleVal("biaotihou"), 0);
                    String pic = subContent(jiequContent, getRuleVal("tupianqian"), getRuleVal("tupianhou"), 0);
                    pic = Misc.fixUrl(webUrl, pic);
                    String link = subContent(jiequContent, getRuleVal("lianjieqian"), getRuleVal("lianjiehou"), 0);
                    JSONObject v = new JSONObject();
                    v.put("vod_id", title + "$$$" + pic + "$$$" + link);
                    v.put("vod_name", title);
                    v.put("vod_pic", pic);
                    v.put("vod_remarks", "");
                    videos.put(v);
                    jiequContent = subContent(parseContent, jiequshuzuqian, jiequshuzuhou, jiequStartIdx);
                } catch (Throwable th) {
                    break;
                }
            }
            JSONObject result = new JSONObject();
            result.put("page", pg);
            result.put("pagecount", Integer.MAX_VALUE);
            result.put("limit", 90);
            result.put("total", Integer.MAX_VALUE);
            result.put("list", videos);
            return result;
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return null;
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        JSONObject obj = category(tid, pg, filter, extend);
        return obj != null ? obj.toString() : "";
    }

    @Override
    public String detailContent(List<String> ids) {
        try {
            fetchRule();
            String[] idInfo = ids.get(0).split("\\$\\$\\$");
            String webUrl = getRuleVal("url") + idInfo[2];
            String html = fetch(webUrl);
            String parseContent = html;
            boolean bfshifouercijiequ = getRuleVal("bfshifouercijiequ").equals("1");
            if (bfshifouercijiequ) {
                String jiequqian = getRuleVal("bfjiequqian");
                String jiequhou = getRuleVal("bfjiequhou");
                parseContent = subContent(html, jiequqian, jiequhou, 0);
            }

            ArrayList<String> playList = new ArrayList<>();

            String jiequshuzuqian = getRuleVal("bfjiequshuzuqian");
            String jiequshuzuhou = getRuleVal("bfjiequshuzuhou");
            resetSubIdx();
            boolean bfyshifouercijiequ = getRuleVal("bfyshifouercijiequ").equals("1");
            String jiequContent = subContent(parseContent, jiequshuzuqian, jiequshuzuhou, 0);
            while (subContentIdx[1] != -1) {
                try {
                    int jiequStartIdx = subContentIdx[1];
                    String parseJqContent = bfyshifouercijiequ ? subContent(jiequContent, getRuleVal("bfyjiequqian"), getRuleVal("bfyjiequhou"), 0) : jiequContent;
                    resetSubIdx();
                    String lastParseContent = subContent(parseJqContent, getRuleVal("bfyjiequshuzuqian"), getRuleVal("bfyjiequshuzuhou"), 0);
                    List<String> vodItems = new ArrayList<>();
                    while (subContentIdx[1] != -1) {
                        int jiequStartIdxSub = subContentIdx[1];
                        String title = subContent(lastParseContent, getRuleVal("bfbiaotiqian"), getRuleVal("bfbiaotihou"), 0);
                        String link = subContent(lastParseContent, getRuleVal("bflianjieqian"), getRuleVal("bflianjiehou"), 0);
                        vodItems.add(title + "$" + link);
                        lastParseContent = subContent(parseJqContent, getRuleVal("bfyjiequshuzuqian"), getRuleVal("bfyjiequshuzuhou"), jiequStartIdxSub);
                    }
                    playList.add(TextUtils.join("#", vodItems));
                    jiequContent = subContent(parseContent, jiequshuzuqian, jiequshuzuhou, jiequStartIdx);
                } catch (Throwable th) {
                    break;
                }
            }

            String cover = idInfo[1], title = idInfo[0], desc = "", category = "", area = "", year = "", remark = "", director = "", actor = "";


            JSONObject vod = new JSONObject();
            vod.put("vod_id", ids.get(0));
            vod.put("vod_name", title);
            vod.put("vod_pic", cover);
            vod.put("type_name", category);
            vod.put("vod_year", year);
            vod.put("vod_area", area);
            vod.put("vod_remarks", remark);
            vod.put("vod_actor", actor);
            vod.put("vod_director", director);
            vod.put("vod_content", desc);

            ArrayList<String> playFrom = new ArrayList<>();

            for (int i = 0; i < playList.size(); i++) {
                playFrom.add("播放列表" + (i + 1));
            }

            String vod_play_from = TextUtils.join("$$$", playFrom);
            String vod_play_url = TextUtils.join("$$$", playList);
            vod.put("vod_play_from", vod_play_from);
            vod.put("vod_play_url", vod_play_url);

            JSONObject result = new JSONObject();
            JSONArray list = new JSONArray();
            list.put(vod);
            result.put("list", list);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            fetchRule();
            String webUrl = getRuleVal("url") + id;
            JSONObject result = new JSONObject();
            result.put("parse", 1);
            result.put("playUrl", "");
            result.put("url", webUrl);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    @Override
    public String searchContent(String key, boolean quick) {
        try {
            fetchRule();
            boolean ssmoshiJson = getRuleVal("ssmoshi").equals("0");
            String webUrl = getRuleVal("url") + getRuleVal("sousuoqian") + key + getRuleVal("sousuohou");
            String webContent = fetch(webUrl);
            JSONObject result = new JSONObject();
            JSONArray videos = new JSONArray();
            if (ssmoshiJson) {
                JSONObject data = new JSONObject(webContent);
                JSONArray vodArray = data.getJSONArray("list");
                for (int j = 0; j < vodArray.length(); j++) {
                    JSONObject vod = vodArray.getJSONObject(j);
                    String name = vod.optString(getRuleVal("jsname")).trim();
                    String id = vod.optString(getRuleVal("jsid")).trim();
                    String pic = vod.optString(getRuleVal("jspic")).trim();
                    pic = Misc.fixUrl(webUrl, pic);
                    JSONObject v = new JSONObject();
                    v.put("vod_id", name + "$$$" + pic + "$$$" + getRuleVal("sousuohouzhui") + id);
                    v.put("vod_name", name);
                    v.put("vod_pic", pic);
                    v.put("vod_remarks", "");
                    videos.put(v);
                }
            } else {
                String parseContent = webContent;
                boolean shifouercijiequ = getRuleVal("sousuoshifouercijiequ").equals("1");
                if (shifouercijiequ) {
                    String jiequqian = getRuleVal("ssjiequqian");
                    String jiequhou = getRuleVal("ssjiequhou");
                    parseContent = subContent(webContent, jiequqian, jiequhou, 0);
                }
                String jiequshuzuqian = getRuleVal("ssjiequshuzuqian");
                String jiequshuzuhou = getRuleVal("ssjiequshuzuhou");
                resetSubIdx();
                String jiequContent = subContent(parseContent, jiequshuzuqian, jiequshuzuhou, 0);
                while (subContentIdx[1] != -1) {
                    try {
                        int jiequStartIdx = subContentIdx[1];
                        String title = subContent(jiequContent, getRuleVal("ssbiaotiqian"), getRuleVal("ssbiaotihou"), 0);
                        String pic = subContent(jiequContent, getRuleVal("sstupianqian"), getRuleVal("sstupianhou"), 0);
                        pic = Misc.fixUrl(webUrl, pic);
                        String link = subContent(jiequContent, getRuleVal("sslianjieqian"), getRuleVal("sslianjiehou"), 0);
                        JSONObject v = new JSONObject();
                        v.put("vod_id", title + "$$$" + pic + "$$$" + link);
                        v.put("vod_name", title);
                        v.put("vod_pic", pic);
                        v.put("vod_remarks", "");
                        videos.put(v);
                        jiequContent = subContent(parseContent, jiequshuzuqian, jiequshuzuhou, jiequStartIdx);
                    } catch (Throwable th) {
                        break;
                    }
                }
            }
            result.put("list", videos);
            return result.toString();
        } catch (
                Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    protected String ext = null;
    protected JSONObject rule = null;

    protected void fetchRule() {
        if (rule == null) {
            if (ext != null) {
                try {
                    if (ext.startsWith("http")) {
                        String json = OkHttpUtil.string(ext, null);
                        rule = new JSONObject(json);
                    } else {
                        rule = new JSONObject(ext);
                    }
                } catch (JSONException e) {
                }
            }
        }
    }

    protected String fetch(String webUrl) {
        SpiderDebug.log(webUrl);
        return OkHttpUtil.string(webUrl, getHeaders(webUrl));
    }

    private int[] subContentIdx = new int[2];

    private void resetSubIdx() {
        subContentIdx[0] = -1;
        subContentIdx[1] = -1;
    }

    private String getRuleVal(String key, String defaultVal) {
        String v = rule.optString(key);
        if (v.isEmpty() || v.equals("空"))
            return defaultVal;
        return v;
    }

    private String getRuleVal(String key) {
        return getRuleVal(key, "");
    }

    private String subContent(String content, String startFlag, String endFlag, int startIdx) {
        try {
            int start = content.indexOf(startFlag, startIdx);
            if (start == -1) {
                resetSubIdx();
                return "";
            }
            start = start + startFlag.length();
            int end = content.indexOf(endFlag, start);
            subContentIdx[0] = start;
            subContentIdx[1] = end;
            return content.substring(start, end);
        } catch (Throwable th) {
            resetSubIdx();
            return "";
        }
    }
}
