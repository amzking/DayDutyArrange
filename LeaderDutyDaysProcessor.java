package com.ccue.cmim.dispatch.onduty.engine.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.hi.baseservice.model.ShiftInfo;

import com.ccue.cmim.dispatch.basics.model.DisOnDutyLerder;
import com.ccue.cmim.dispatch.basics.model.LeaderLevel;
import com.ccue.cmim.dispatch.onduty.engine.ClassAndOndutyType;
import com.ccue.cmim.dispatch.onduty.engine.DayArrange;

/**
 * @author zk
 *
 */
public class LeaderDutyDaysProcessor extends AbstractSchedulingProcessor {

	private HashSet<DisOnDutyLerder> viceBossSet;

	@Override
	public List<DayArrange> process(Integer leaOndPlanId,
			List<DayArrange> arrange, List<DisOnDutyLerder> mz,
			List<DisOnDutyLerder> vz, List<DisOnDutyLerder> md,
			List<DisOnDutyLerder> vd) {

		// 大矿重排，副矿重排，副总重排
		
		Map<DisOnDutyLerder, Integer> mainChief = new HashMap<DisOnDutyLerder, Integer>();
		Map<DisOnDutyLerder, Integer> viceChiefs = new HashMap<DisOnDutyLerder, Integer>();
		Map<DisOnDutyLerder, Integer> viceBosses = new HashMap<DisOnDutyLerder, Integer>();
		dutyTimesCaculate(arrange, mainChief, viceChiefs, viceBosses);
		
		//处理大矿的带班次数
		for(Map.Entry<DisOnDutyLerder, Integer> mc : mainChief.entrySet()){
			while(mc.getValue() != 6){
				//使大矿满足次数6
				if(mc.getValue() > 6){
					for(DayArrange da : arrange){
						Map<ShiftInfo, List<DisOnDutyLerder>> dutyLeaders = da.getLeads();
						for (Map.Entry<ShiftInfo, List<DisOnDutyLerder>> sild : dutyLeaders
								.entrySet()) {
							if (sild.getValue().get(0).getLeaderLevel() == LeaderLevel.LEADERLEVEL_LL_DK) {
								//减少该领导带班次数，新换的领导满足：1不在当天出现，2与副职专业不同
//								exchangeZD(da, dutyLeaders, viceBossSet, times);
							}
						}
					}
				} else if(mc.getValue() < 6){
					
				}
			}
		}
		
		return null;
	}

	private void exchangeZD(DayArrange da,
			HashSet<DisOnDutyLerder> viceBossSet2, int times) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 统计大矿，副矿，副总各带班（主）次数
	 * 
	 * @param arrange
	 * @param mainChief
	 */
	private void dutyTimesCaculate(List<DayArrange> arrange,
			Map<DisOnDutyLerder, Integer> mainChief,
			Map<DisOnDutyLerder, Integer> viceChiefs,
			Map<DisOnDutyLerder, Integer> viceBosses) {

		for (DayArrange da : arrange) {
			Map<ShiftInfo, List<DisOnDutyLerder>> dutyLeaders = da.getLeads();
			for (Map.Entry<ShiftInfo, List<DisOnDutyLerder>> sild : dutyLeaders
					.entrySet()) {
				if (sild.getValue().size() > 0) {
					DisOnDutyLerder leader = sild.getValue().get(0);
					if (leader.getLeaderLevel() == LeaderLevel.LEADERLEVEL_LL_DK)
						if (!mainChief.containsKey(leader)) {
							mainChief.put(leader, 1);
						} else {
							mainChief.put(leader, mainChief.get(leader) + 1);
						}
					if (leader.getLeaderLevel() == LeaderLevel.LEADERLEVEL_LL_FK)
						if (!viceChiefs.containsKey(leader)) {
							viceChiefs.put(leader, 1);
						} else {
							viceChiefs.put(leader, viceChiefs.get(leader) + 1);
						}
					if (leader.getLeaderLevel() == LeaderLevel.LEADERLEVEL_LL_FZ) {
						viceBossSet = new HashSet<DisOnDutyLerder>();
						viceBossSet.add(leader);
						if (!viceBosses.containsKey(leader)) {
							viceBosses.put(leader, 1);
						} else {
							viceBosses.put(leader, viceBosses.get(leader) + 1);
						}
					}
				}
			}
		}
	}

	/**
	 * 建立规则 判断是否满足换后，领导主带班次数6次，9次
	 * ，10次-11次，先判断大矿是否是6次，若否，与副总换，再判断每个副矿是否是9次，否与副总换。 一个月的总的带班次数，主带：3*30=90
	 * 6*1+9*4+ 10(or 11)*5 =92~97
	 */
	@Override
	public boolean macth(List<DayArrange> arrange, DayArrange so, DayArrange to,
			ClassAndOndutyType sType, ClassAndOndutyType tType,
			DisOnDutyLerder s, DisOnDutyLerder t) {
		Map<DisOnDutyLerder, Integer> mainChief = new HashMap<DisOnDutyLerder, Integer>();
		Map<DisOnDutyLerder, Integer> viceChiefs = new HashMap<DisOnDutyLerder, Integer>();
		Map<DisOnDutyLerder, Integer> viceBosses = new HashMap<DisOnDutyLerder, Integer>();
		dutyTimesCaculate(arrange, mainChief, viceChiefs, viceBosses);
		
		boolean isDKSatisfied = false;
		boolean isFKSatisfied = true;
		for(Map.Entry<DisOnDutyLerder, Integer> mc : mainChief.entrySet()){
			if (mc.getValue() == 6 ) {
				isDKSatisfied = true;
			}
		}
		for(Map.Entry<DisOnDutyLerder, Integer> vc : viceChiefs.entrySet()){
			if(vc.getValue() != 9)
				isFKSatisfied = false;
		}
		if(isDKSatisfied && isFKSatisfied)
			return true;
		return false;
	}

	@Override
	public String name() {
		return "LeaderDutyDays";
	}

}
