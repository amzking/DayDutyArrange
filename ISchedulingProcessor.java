package com.ccue.cmim.dispatch.onduty.engine;

import java.util.List;

import com.ccue.cmim.dispatch.basics.model.DisOnDutyLerder;

public interface ISchedulingProcessor {
	public List<DayArrange> process(Integer leaOndPlanId, List<DayArrange> arrange, List<DisOnDutyLerder> mz, List<DisOnDutyLerder> vz, List<DisOnDutyLerder> md, List<DisOnDutyLerder> vd);

	public boolean macth(List<DayArrange> arrange, DayArrange so, DayArrange to, ClassAndOndutyType sType, ClassAndOndutyType tType, DisOnDutyLerder s, DisOnDutyLerder t);
	
	public String name();
}
