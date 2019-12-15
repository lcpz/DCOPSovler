package edu.cqu.result;

public class ResultWithPrivacy extends ResultAls {

	public int leakedEntropy;
	public int totalEntropy;
	public int ub;
	public long messageSizeCount;

	public void setLeakedEntropy(int e) {
		leakedEntropy = e;
	}

	public void setTotalEntropy(int e) {
		totalEntropy = e;
	}

	public void setMessageSizeCount(long l) {
		messageSizeCount = l;
	}

	public void setUb(int u) {
		ub = u;
	}

}
