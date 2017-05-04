package interfaceApplication;

import java.util.HashMap;

import org.json.simple.JSONObject;

import esayhelper.JSONHelper;
import esayhelper.TimeHelper;
import model.taskModel;
import rpc.execRequest;

public class task {
	private taskModel model = new taskModel();
	private HashMap<String, Object> map = new HashMap<>();
	private String userid;

	public task() {
		userid = execRequest.getChannelValue("Userid").toString();

		map.put("timediff", 3); // 更新时间周期，单位为天
		map.put("lasttime", String.valueOf(TimeHelper.nowMillis()));
		map.put("state", 0); // 0：未开始；1：进行中；2：已结束；3：已取消
		map.put("type", 0);
		map.put("ownid", 0); // 所有者id
		map.put("priority", 0); // 任务优先级
	}

	public String TaskAdd(String info) {
		String tip = execRequest
				._run("GrapeAuth/Auth/InsertPLV/s:" + userid, null).toString();
		if (!"0".equals(tip)) {
			return model.resultMessage(2, "");
		}
		JSONObject object = model.AddMap(map, JSONHelper.string2json(info));
		return model.resultMessage(JSONHelper.string2json(model.Add(object)));
	}

	public String TaskUpdate(String id, String info) {
		String uPLV = model.find(id).get("uplv").toString();
		String tip = execRequest
				._run("GrapeAuth/Auth/UpdatePLV/s:" + uPLV + "/s:" + userid,
						null)
				.toString();
		if (!"0".equals(tip)) {
			return model.resultMessage(3, "没有编辑权限");
		}
		return model.resultMessage(
				model.update(id, JSONHelper.string2json(info)), "任务修改成功");
	}

	public String TaskDelete(String id) {
		String dPLV = model.find(id).get("dplv").toString();
		String tip = execRequest
				._run("GrapeAuth/Auth/DeletePLV/s:" + dPLV + "/s:" + userid,
						null)
				.toString();
		if (!"0".equals(tip)) {
			return model.resultMessage(3, "没有编辑权限");
		}
		return model.resultMessage(model.delete(id), "任务删除成功");
	}

	public String TaskBatchDelete(String ids) {
		return model.resultMessage(model.delete(ids.split(",")), "任务批量删除成功");
	}

	public String TaskPage(int ids, int pageSize) {
		return model.resultMessage(model.page(ids, pageSize));
	}

	public String TaskPageBy(int ids, int pageSize, String info) {
		return model.resultMessage(
				model.page(ids, pageSize, JSONHelper.string2json(info)));
	}

	// 需要通知栏目所有人更新栏目
	public String TaskNotice(String username) {
		return model.resultMessage(model.notice(username));
	}
}
