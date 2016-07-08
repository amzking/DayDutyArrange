package com.ccue.cmim.dispatch.onduty.engine;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hi.baseservice.model.ShiftInfo;

import com.ccue.cmim.dispatch.basics.model.DisOnDutyLerder;

public class DayArrange {
	private Date day;
	private DisOnDutyLerder main;
	private List<DisOnDutyLerder> vices = new LinkedList<DisOnDutyLerder>();
	private Map<ShiftInfo,List<DisOnDutyLerder>> leads = new HashMap<ShiftInfo, List<DisOnDutyLerder>>();
	
	public DayArrange(Date day, List<ShiftInfo> sis){
		this.day = day;
		for (ShiftInfo si : sis) {
			leads.put(si, new LinkedList<DisOnDutyLerder>());
		}
	}
	
	public Date getDay() {
		return day;
	}
	public void setDay(Date day) {
		this.day = day;
	}
	public DisOnDutyLerder getMain() {
		return main;
	}
	public void setMain(DisOnDutyLerder main) {
		this.main = main;
	}
	public List<DisOnDutyLerder> getVices() {
		return vices;
	}
	public void setVices(List<DisOnDutyLerder> vices) {
		this.vices = vices;
	}

	public Map<ShiftInfo, List<DisOnDutyLerder>> getLeads() {
		return leads;
	}

	public void setLeads(Map<ShiftInfo, List<DisOnDutyLerder>> leads) {
		this.leads = leads;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("值班:").append(main.getLeadername().getFullName()).append(",");
		for(int i = 0; i < vices.size(); i++){
			if(i > 0)
				sb.append(",");
			sb.append(vices.get(i).getLeadername().getFullName());	
		}
		sb.append("\r\n");
		int i = 0;
		for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : leads.entrySet()){
			if(i > 0)
				sb.append("\r\n");
			sb.append(entry.getKey().getSiName()).append(":");
			List<DisOnDutyLerder> dls = entry.getValue();
			for(int j = 0; j < dls.size(); j++){
				if(j > 0)
					sb.append(",");
				sb.append(dls.get(j).getLeadername().getFullName());
			}
			i++;
		}
		return sb.toString();
	}
	
	
}
