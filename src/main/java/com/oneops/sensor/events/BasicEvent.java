package com.oneops.sensor.events;

import java.io.Serializable;

public class BasicEvent implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public static final String DEFAULT_CHANNEL = "default_channel";
	private long ciId;
	private long timestamp;
	private String source;
	private long manifestId;
	private long checksum;
	private String bucket;
	private PerfEventPayload metrics = new PerfEventPayload();
	private PerfEventPayload offsets = new PerfEventPayload();
	private String grouping;
	private String channel;
	private boolean isAggregate = false;
	
	public String getGrouping() {
		return grouping;
	}
	
	public void setGrouping(String grouping) {
		this.grouping = grouping;
	}
	
	public long getManifestId() {
		return manifestId;
	}

	public void setManifestId(long manifestId) {
		this.manifestId = manifestId;
	}

	public long getChecksum() {
		return checksum;
	}

	public void setChecksum(long checksum) {
		this.checksum = checksum;
	}

	public PerfEventPayload getMetrics() {
		return metrics;
	}

	public void setMetrics(PerfEventPayload metrics) {
		this.metrics = metrics;
	}

	public PerfEventPayload getOffsets() {
		return offsets;
	}

	public void setOffsets(PerfEventPayload offsets) {
		this.offsets = offsets;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public void setCiId(long ciId) {
		this.ciId = ciId;
	}

	public long getCiId() {
		return ciId;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSource() {
		return source;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getChannel() {
		if (this.channel != null) {
			return channel;
		} else {
			return DEFAULT_CHANNEL;
		}
		 
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public boolean isAggregate() {
		return isAggregate;
	}

	public void setAggregate(boolean isAggregate) {
		this.isAggregate = isAggregate;
	}
	
}
