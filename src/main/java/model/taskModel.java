package model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import esayhelper.DBHelper;
import esayhelper.JSONHelper;
import esayhelper.TimeHelper;
import esayhelper.formHelper;
import esayhelper.jGrapeFW_Message;
import esayhelper.formHelper.formdef;
import session.session;

public class taskModel {
	private static DBHelper dbtask;
	private static formHelper form;
	static {
		dbtask = new DBHelper("mongodb", "task");
		form = dbtask.getChecker();
	}

	public taskModel() {
		form.putRule("name", formdef.notNull);
		form.putRule("timediff", formdef.notNull);
	}

	public String Add(JSONObject Info) {
		if (!form.checkRuleEx(Info)) {
			return resultMessage(1, "必填项为空");
		}
		String tips = dbtask.data(Info).insertOnce().toString();
		return find(tips).toString();
	}

	public int update(String id, JSONObject Info) {
//		JSONObject object = getInt(Info);
		return dbtask.eq("_id", new ObjectId(id)).data(Info).update() != null ? 0 : 99;
	}

	public int delete(String id) {
		return dbtask.eq("_id", new ObjectId(id)).delete() != null ? 0 : 99;
	}

	public int delete(String[] ids) {
//		int dplv=0;
		dbtask.or();
		for (int i = 0, len = ids.length; i < len; i++) {
//			dplv = Integer.parseInt(getPLV(mids[i]).get("dplv").toString());
//			if (userplv<dplv) {
//				continue;
//			}
			dbtask.eq("_id", new ObjectId(ids[i]));
		}
		return dbtask.deleteAll() == ids.length ? 0 : 99;
	}

	public JSONArray find(JSONObject info) {
		for (Object object2 : info.keySet()) {
			dbtask.like(object2.toString(), info.get(object2.toString()));
		}
		return dbtask.limit(30).select();
	}
	public JSONObject find(String taskid) {
		return dbtask.eq("_id", new ObjectId(taskid)).find();
	}

	@SuppressWarnings("unchecked")
	public JSONObject page(int ids, int pageSize) {
		JSONArray array = dbtask.page(ids, pageSize);
		JSONObject object = new JSONObject();
		object.put("totalSize", (int) Math.ceil((double) dbtask.count() / pageSize));
		object.put("currentPage", ids);
		object.put("pageSize", pageSize);
		object.put("data", array);
		return object;
	}

	@SuppressWarnings("unchecked")
	public JSONObject page(int ids, int pageSize, JSONObject info) {
		for (Object object2 : info.keySet()) {
			dbtask.eq(object2.toString(), info.get(object2.toString()));
		}
		JSONArray array = dbtask.page(ids, pageSize);
		JSONObject object = new JSONObject();
		object.put("totalSize", (int) Math.ceil((double) dbtask.count() / pageSize));
		object.put("currentPage", ids);
		object.put("pageSize", pageSize);
		object.put("data", array);
		return object;
	}

	@SuppressWarnings("unchecked")
	public JSONArray notice(String username) {
		// 获取当前登录用户
		session session = new session();
		String info = session.get(username).toString();
		String ownid = JSONHelper.string2json(info).get("ownid").toString();
		JSONArray array = dbtask.eq("ownid", ownid).limit(20).select();
		JSONArray arrays = new JSONArray();
		String currentTime = String.valueOf(TimeHelper.nowMillis());
		for (int i = 0, len = array.size(); i < len; i++) {
			JSONObject object = (JSONObject) array.get(i);
			String lasttime = object.get("lasttime").toString();
			int diff = (int) Math
					.ceil((double) (Long.parseLong(currentTime) - Long.parseLong(lasttime)) / (1000 * 60 * 60 * 24));
			int timediff = Integer.parseInt(object.get("timediff").toString());
			if (diff <= timediff) {
				continue;
			}
			arrays.add(object);
		}
		return arrays;
	}

//	@SuppressWarnings("unchecked")
//	public JSONObject getInt(JSONObject object) {
//		int value;
//		for (Object object2 : object.keySet()) {
//			if (object.get(object2.toString()) instanceof Long) {
//				value = Integer.parseInt(String.valueOf(object.get(object2.toString())));
//				object.put(object2.toString(), value);
//			}
//		}
//		return object;
//	}
	public String getID() {
		String str = UUID.randomUUID().toString();
		return str.replace("-", "");
	}

	/**
	 * 将map添加至JSONObject中
	 * 
	 * @param map
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject AddMap(HashMap<String, Object> map, JSONObject object) {
		if (map.entrySet() != null) {
			Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iterator.next();
				if (!object.containsKey(entry.getKey())) {
					object.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return object;
	}

	public String resultMessage(int num, String message) {
		String msg = "";
		switch (num) {
		case 0:
			msg = message;
			break;
		case 1:
			msg = "必填项没有填";
			break;
		case 2:
			msg = "没有创建数据权限，请联系管理员进行权限调整";
			break;
		case 3:
			msg = "没有修改数据权限，请联系管理员进行权限调整";
			break;
		case 4:
			msg = "没有删除数据权限，请联系管理员进行权限调整";
			break;
		default:
			msg = "其它异常";
			break;
		}
		return jGrapeFW_Message.netMSG(num, msg);
	}
}
