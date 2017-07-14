package interfaceApplication;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import JGrapeSystem.jGrapeFW_Message;
import apps.appsProxy;
import nlogger.nlogger;
import rpc.execRequest;
import session.session;
import string.StringHelper;
import time.TimeHelper;

public class task {
	private static session se;
	private JSONObject UserInfo = null;
	private String sid = null;
	private static boolean initThread;
	private Timer timer = new Timer();

	static {
		se = new session();
		initThread = false;
	}

	public task() {
		sid = (String) execRequest.getChannelValue("sid");

		if (sid != null) {
			UserInfo = new JSONObject();
			UserInfo = se.getSession(sid);
		}
	}

	/**
	 * 
	 * 超过指定时间未更新的栏目，短信通知管理员进行更新
	 * 
	 * @project GrapeTask
	 * @package interfaceApplication
	 * @file task.java
	 * 
	 * @param taskInfo
	 *            每月几号几点{"day":5,"hour":9,"ogid":"","type":0,"timediff":3}
	 *            每周几，几点{"day":1,"hour":9,"ogid":"","type":1,"timediff":3}
	 *            每天几点{"day":0,"hour":9,"ogid":"","type":2,"timediff":3}
	 * @return
	 *
	 */
	public String AddTask(String taskInfo) {
		int type = -1;
		String result = resultMessage(99);
		JSONObject object = JSONObject.toJSON(taskInfo);
		if (object != null) {
			int day = Integer.parseInt(object.getString("day"));
			int hour = Integer.parseInt(object.getString("hour"));
			String ogid = object.getString("ogid");
			int timediff = Integer.parseInt(object.getString("timediff")); // 时间周期
			if (IsNotice(ogid, timediff)) {
				type = Integer.parseInt(object.getString("type"));
				noticeManager(day, hour, type);
				result = resultMessage(1);
			}
		}
		return result;
	}

	/**
	 * 按周显示待更新的文章
	 * @project	GrapeTask
	 * @package interfaceApplication
	 * @file task.java
	 * 
	 * @return  [{"rest":4,"name":"通知公告"}]
	 *			[{"rest":"本周还需更新的文章数量","name":"栏目名称"}]
	 */
	@SuppressWarnings("unchecked")
	public String Show() {
		int total = 5; // 每周需更新5篇文章
		int count = 0;
		System.out.println(UserInfo);
		if (UserInfo == null || UserInfo.size() == 0) {
			return resultMessage(2);
		}
		// 获取该用户下的所能管理的所有栏目及网站
		String wbid = UserInfo.getString("currentWeb");
		JSONObject object = (JSONObject) UserInfo.get("_id");
		String ownid = (String) object.get("$oid");
		String condString = "[{\"field\":\"ownid\",\"logic\":\"like\",\"value\":\"" + ownid
				+ "\"},{\"field\":\"wbid\",\"logic\":\"like\",\"value\":\"" + wbid + "\"}]";
		// 获取栏目
		System.out.println(getHost(0));
		String column = appsProxy
				.proxyCall(getHost(0), appsProxy.appid() + "/15/ContentGroup/getColumnId/" + condString, null, null)
				.toString();
		JSONArray array = JSONArray.toJSONArray(column);
		JSONObject obj = getColumn(array);
		for (int i = 0; i < array.size(); i++) {
			object = (JSONObject) array.get(i);
			count = Integer.parseInt((String) obj.get(object.get("_id")));
			object.put("rest", total - count);
			object.remove("_id");
			array.set(i, object);
		}
		return array.toJSONString();
	}

	private JSONObject getColumn(JSONArray array) {
		JSONObject object = new JSONObject();
		String column = "";
		String condString;
		if (array != null && array.size() != 0) {
			// 获取栏目id
			for (Object object2 : array) {
				object = (JSONObject) object2;
				column += object.get("_id") + ",";
			}
			if (column.length() > 0) {
				column = StringHelper.fixString(column, ',');
			}
			// 获取每个栏目下已更新了多少篇文章
			long start = getStamp(2, 0, 0, 0); // 周一 00：00：00
			long end = getStamp(1, 23, 59, 59); // 周日 23：59：59
			condString = "[{\"field\":\"time\",\"logic\":\">=\",\"value\":" + start
					+ "},{\"field\":\"time\",\"logic\":\"<\",\"value\":" + end + "}]";
			// 获取栏目下已更新文章数量
			System.out.println(getHost(0));
			column = appsProxy
					.proxyCall(getHost(0),
							appsProxy.appid() + "/15/Content/getArticleCount/" + condString + "/" + column, null, null)
					.toString();
			object = JSONObject.toJSON(column);
		}
		return object;
	}

	// 获取周的时间戳 0:Sunday。。。
	private Long getStamp(int week, int hour, int minutes, int second) {
		long time = 0;
		try {
			Calendar calendar = Calendar.getInstance();
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
			calendar.set(Calendar.DAY_OF_WEEK, week);
			if (week == 1) {
				calendar.add(Calendar.WEEK_OF_YEAR, 1);
			}
			calendar.set(Calendar.HOUR_OF_DAY, hour);
			calendar.set(Calendar.MINUTE, minutes);
			calendar.set(Calendar.SECOND, second);
			time = TimeHelper.dateToStamp(df.format(calendar.getTime()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return time;
	}

	// 对于超过指定时间未更新的栏目内容，通知栏目管理员进行更新
	private void noticeManager(int day, int hour, int type) {
		Date date = new Date();
		int period = 0;
		int num = 0;
		Calendar calendar = Calendar.getInstance();
		// 获取第一次执行任务的时间
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		switch (type) {
		case 0: // 每月指定时间通知负责人
			// 执行周期
			period = getMonthDay(calendar.get(Calendar.MONTH) * 24 * 60 * 60 * 1000);
			num = getMonthDay(calendar.get(Calendar.MONTH) + 1);
			calendar.set(Calendar.DAY_OF_MONTH, day);
			break;
		case 1: // 每周指定时间通知负责人
			num = 7;
			period = 7 * 24 * 60 * 60 * 1000;
			calendar.set(Calendar.DAY_OF_WEEK, day + 1);
			break;
		case 2: // 每天指定时间通知负责人
			num = 1;
			period = 24 * 60 * 60 * 1000;
			break;
		}
		date = calendar.getTime();
		System.out.println(date);
		if (date.before(new Date())) {
			date = this.addDay(date, num);
		}
		if (initThread == false) {
			initThread = true;
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					nlogger.logout("XXXXXX"); // 发送短信到栏目负责人
				}
			}, date, period);
		}
	}

	// 获取某个月有多少天
	private int getMonthDay(int month) {
		Calendar calendar = Calendar.getInstance();
		if (month > 0 && month < 12) {
			calendar.set(Calendar.MONTH, month - 1);
		}
		return calendar.getActualMaximum(Calendar.DATE);
	}

	/**
	 * 是否需要发送短信通知负责人
	 * 
	 * @project GrapeTask
	 * @package interfaceApplication
	 * @file task.java
	 * 
	 * @param object
	 * @return
	 *
	 */
	private boolean IsNotice(String ogid, int timediff) {
		JSONObject obj;
		long time = 0;
		long currentTime = TimeHelper.nowMillis();
		// 获取该栏目下的最新的文章及栏目管理员的信息[手机号，邮箱....]
		String temp = (String) appsProxy.proxyCall(getHost(0),
				appsProxy.appidString() + "/15/Content/FindNewByOgid/" + ogid, null, null);
		obj = JSONObject.toJSON(temp);
		if (obj != null) {
			time = obj.getLong("time");
		}
		return (currentTime - time) / (1000 * 60 * 60 * 24) > timediff;
	}

	// 增加或减少相应的天数
	private Date addDay(Date date, int num) {
		Calendar startDT = Calendar.getInstance();
		startDT.setTime(date);
		startDT.add(Calendar.DAY_OF_MONTH, num);
		nlogger.logout(startDT.getTime());
		return startDT.getTime();
	}

	/**
	 * 获取URLConfig.properties配置文件中内网url地址，外网url地址
	 * 
	 * @param key
	 * @return
	 *
	 */
	private String getAppIp(String key) {
		String value = "";
		try {
			Properties pro = new Properties();
			pro.load(new FileInputStream("URLConfig.properties"));
			value = pro.getProperty(key);
		} catch (Exception e) {
			value = "";
		}
		return value;
	}

	/**
	 * 获取应用url[内网url或者外网url]，0表示内网，1表示外网
	 * 
	 * @param signal
	 *            0或者1 0表示内网，1表示外网
	 * @return
	 *
	 */
	private String getHost(int signal) {
		String host = null;
		try {
			if (signal == 0 || signal == 1) {
				host = getAppIp("host").split("/")[signal];
			}
		} catch (Exception e) {
			nlogger.logout(e);
			host = null;
		}
		return host;
	}

	private String resultMessage(int num) {
		return resultMessage(num, "");
	}

	private String resultMessage(int num, String message) {
		String msg = "";
		switch (num) {
		case 0:
			msg = message;
			break;
		case 1:
			msg = "已短信通知栏目管理员";
			break;
		case 2:
			msg = "获取待更新文章失败";
			break;
		default:
			break;
		}
		return jGrapeFW_Message.netMSG(num, msg);
	}
}
