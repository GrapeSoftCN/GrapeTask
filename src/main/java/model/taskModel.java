package model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import apps.appsProxy;
import database.db;
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
	private JSONObject _obj = new JSONObject();

	static {
		dbtask = new DBHelper(appsProxy.configValue().get("db").toString(),
				"task");
//		dbtask = new DBHelper("mongodb", "task");
		form = dbtask.getChecker();
	}

	private db bind(){
		return dbtask.bind(String.valueOf(appsProxy.appid()));
	}
	
	public taskModel() {
		form.putRule("name", formdef.notNull);
		form.putRule("timediff", formdef.notNull);
	}

	public String Add(JSONObject Info) {
		if (!form.checkRuleEx(Info)) {
			return resultMessage(1, "必填项为空");
		}
		String tips = bind().data(Info).insertOnce().toString();
		return find(tips).toString();
	}

	public int update(String id, JSONObject Info) {
		return bind().eq("_id", new ObjectId(id)).data(Info).update() != null
				? 0 : 99;
	}

	public int delete(String id) {
		return bind().eq("_id", new ObjectId(id)).delete() != null ? 0 : 99;
	}

	public int delete(String[] ids) {
		bind().or();
		for (int i = 0, len = ids.length; i < len; i++) {
			bind().eq("_id", new ObjectId(ids[i]));
		}
		return bind().deleteAll() == ids.length ? 0 : 99;
	}

	public JSONArray find(JSONObject info) {
		for (Object object2 : info.keySet()) {
			bind().like(object2.toString(), info.get(object2.toString()));
		}
		return bind().limit(30).select();
	}

	public JSONObject find(String taskid) {
		return bind().eq("_id", new ObjectId(taskid)).find();
	}

	@SuppressWarnings("unchecked")
	public JSONObject page(int ids, int pageSize) {
		JSONArray array = bind().page(ids, pageSize);
		JSONObject object = new JSONObject();
		object.put("totalSize",
				(int) Math.ceil((double) bind().count() / pageSize));
		object.put("currentPage", ids);
		object.put("pageSize", pageSize);
		object.put("data", array);
		return object;
	}

	@SuppressWarnings("unchecked")
	public JSONObject page(int ids, int pageSize, JSONObject info) {
		for (Object object2 : info.keySet()) {
			if ("_id".equals(object2.toString())) {
				bind().eq("_id", new ObjectId(info.get("_id").toString()));
			}
			bind().eq(object2.toString(), info.get(object2.toString()));
		}
		JSONArray array = bind().dirty().page(ids, pageSize);
		JSONObject object = new JSONObject();
		object.put("totalSize",
				(int) Math.ceil((double) bind().count() / pageSize));
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
		JSONArray array = bind().eq("ownid", ownid).limit(20).select();
		JSONArray arrays = new JSONArray();
		String currentTime = String.valueOf(TimeHelper.nowMillis());
		for (int i = 0, len = array.size(); i < len; i++) {
			JSONObject object = (JSONObject) array.get(i);
			String lasttime = object.get("lasttime").toString();
			int diff = (int) Math.ceil((double) (Long.parseLong(currentTime)
					- Long.parseLong(lasttime)) / (1000 * 60 * 60 * 24));
			int timediff = Integer.parseInt(object.get("timediff").toString());
			if (diff <= timediff) {
				continue;
			}
			arrays.add(object);
		}
		return arrays;
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
			Iterator<Entry<String, Object>> iterator = map.entrySet()
					.iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Object> entry = (Map.Entry<String, Object>) iterator
						.next();
				if (!object.containsKey(entry.getKey())) {
					object.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return object;
	}

	@SuppressWarnings("unchecked")
	public String resultMessage(JSONObject object) {
		_obj.put("records", object);
		return resultMessage(0, _obj.toString());
	}

	@SuppressWarnings("unchecked")
	public String resultMessage(JSONArray array) {
		_obj.put("records", array);
		return resultMessage(0, _obj.toString());
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
