package com.ccue.cmim.dispatch.onduty.engine.impl;

/**
 * zk：于6月30日，增加process函数参数 List<DayArrange> arrange,其余未改
 */


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hi.baseservice.model.ShiftInfo;

import com.ccue.cmim.dispatch.basics.model.DisOnDutyLerder;
import com.ccue.cmim.dispatch.onduty.engine.ClassAndOndutyType;
import com.ccue.cmim.dispatch.onduty.engine.DayArrange;

public class ProfessionalSchedulingProcessor extends
		AbstractSchedulingProcessor {

	@Override
	public List<DayArrange> process(Integer leaOndPlanId,
			List<DayArrange> arrange, List<DisOnDutyLerder> mz,
			List<DisOnDutyLerder> vz, List<DisOnDutyLerder> md,
			List<DisOnDutyLerder> vd) {
		
		for (int i = 0; i< arrange.size(); i++) {
			DayArrange so = arrange.get(i);
			for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : so.getLeads().entrySet()){
				List<DisOnDutyLerder> dutys = new ArrayList<DisOnDutyLerder>();
				dutys.add(so.getMain());
				dutys.addAll(so.getVices());
				dutys = same(dutys);
				//值班专业排重
				for (DisOnDutyLerder s : dutys) {
					ClassAndOndutyType sType = getType(so, s);
					exchang(arrange, i, 1, so, sType, s);
				}
				
				//带班专业排重
				List<DisOnDutyLerder> leads = same(entry.getValue());
				for (DisOnDutyLerder s : leads) {
					ClassAndOndutyType sType = getType(so, s);
					exchang(arrange, i, 1, so, sType, s);
				}
			}
		}
		return arrange;
	}

	@Override
	public boolean macth(List<DayArrange> arrange, DayArrange so, DayArrange to,
			ClassAndOndutyType sType, ClassAndOndutyType tType,
			DisOnDutyLerder s, DisOnDutyLerder t) {
		boolean exit = false, same = false;
		for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : so.getLeads().entrySet()){
			exit = false; same = false;
			List<DisOnDutyLerder> sClscc = entry.getValue();
			for (DisOnDutyLerder l : sClscc) {
				if(l.equals(s)){
					exit = true;
					continue;
				}
				if(l.getProfessional().equals(t.getProfessional()))
					same = true;
			}
			if(exit && same)
				return false;
		}
		for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : to.getLeads().entrySet()){
			exit = false; same = false;
			List<DisOnDutyLerder> tLeads = entry.getValue();
			for (DisOnDutyLerder l : tLeads) {
				if(l.equals(t)) {
					exit = true;
					continue;
				}
				if(l.getProfessional().equals(s.getProfessional()))
					same = true;
			}
			if(exit && same)
				return false;
		}
		
		return true;
	}

	private List<DisOnDutyLerder> same(List<DisOnDutyLerder> clscc){
		Set<Integer> pro = new HashSet<Integer>();
		List<DisOnDutyLerder> result = new ArrayList<DisOnDutyLerder>();
		for(DisOnDutyLerder l : clscc){
			if(pro.contains(l.getProfessional()))
				result.add(l);
			pro.add(l.getProfessional());
		}

		return result;
	}
	@Override
	public String name() {
		return "Professional";
	}

}
