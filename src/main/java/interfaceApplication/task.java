package interfaceApplication;

import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import esayhelper.JSONHelper;
import esayhelper.TimeHelper;
import model.taskModel;
import session.session;

public class task {
	private taskModel model = new taskModel();
	private HashMap<String, Object> map = new HashMap<>();
	private JSONObject _obj = new JSONObject();

	public task() {
		map.put("timediff", 3); // 更新时间周期，单位为天
		map.put("lasttime", String.valueOf(TimeHelper.nowMillis()));
		map.put("state", 0);
		map.put("type", 0);
		map.put("ownid", 0);  //栏目所有者id
	}

	public String TaskAdd(String info) {
		JSONObject object = model.AddMap(map, JSONHelper.string2json(info));
		return model.resultMessage(model.Add(object), "任务新增成功");
	}

	public String TaskUpdate(String id, String info) {
		return model.resultMessage(model.update(id, JSONHelper.string2json(info)), "任务修改成功");
	}

	public String TaskDelete(String id) {
		return model.resultMessage(model.delete(id), "任务删除成功");
	}

	public String TaskBatchDelete(String ids) {
		return model.resultMessage(model.delete(ids.split(",")), "任务批量删除成功");
	}

	@SuppressWarnings("unchecked")
	public String TaskPage(int ids, int pageSize) {
		_obj.put("records", model.page(ids, pageSize));
		return model.resultMessage(0, _obj.toString());
	}

	@SuppressWarnings("unchecked")
	public String TaskPageBy(int ids, int pageSize, String info) {
		_obj.put("records", model.page(ids, pageSize, JSONHelper.string2json(info)));
		return model.resultMessage(0, _obj.toString());
	}

	// 需要通知栏目所有人更新栏目
	@SuppressWarnings("unchecked")
	public String TaskNotice(String username) {
		_obj.put("records", model.notice(username));
		return model.resultMessage(0, _obj.toString());
	}
}
