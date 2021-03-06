package com.fly.common.query.entity;

import org.apache.commons.digester3.annotations.rules.ObjectCreate;
import org.apache.commons.digester3.annotations.rules.SetNext;

import java.util.ArrayList;
import java.util.List;

@ObjectCreate(pattern="queryContext/query/beforeInit")
public class BeforeInit {
	List<BeforeCall> beforeCallList;
	public BeforeInit(){
		beforeCallList=new ArrayList<BeforeCall>();
	}
	public void setBeforeCallList(List<BeforeCall> beforeCallList) {
		this.beforeCallList = beforeCallList;
	}
	@SetNext
	public void addBeforeCall(BeforeCall call){
		beforeCallList.add(call);
	}
	public List<BeforeCall> getBeforeCallList() {
		return beforeCallList;
	}
	
	
}
