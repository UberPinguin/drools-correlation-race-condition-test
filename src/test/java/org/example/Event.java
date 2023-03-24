package org.example;

import java.util.Date;
public class Event {
  Date time;
  String eventIdentifier;
  String location;
  String deviceName;

  public Event(Date time, String eventIdentifier, String location, String deviceName) {
    this.time = time;
    this.eventIdentifier = eventIdentifier;
    this.location = location;
    this.deviceName = deviceName;
  }
  
  public Event clone() {
    return new Event(this.getTime(), this.getEventIdentifier(), this.getLocation(), this.getDeviceName());
  }

  public Date getTime() {
    return time;
  }

  public Event setTime(Date time) {
    this.time = time;
    return this;
  }

  public String getEventIdentifier() {
    return eventIdentifier;
  }

  public Event setEventIdentifier(String eventIdentifier) {
    this.eventIdentifier = eventIdentifier;
    return this;
  }

  public String getLocation() {
    return location;
  }

  public Event setLocation(String location) {
    this.location = location;
    return this;
  }

  public String getDeviceName() {
    return deviceName;
  }

  public Event setDeviceName(String deviceName) {
    this.deviceName = deviceName;
    return this;
  }

  @Override
  public String toString() {
    return "Event{"
        + "time="
        + time
        + ", eventIdentifier='"
        + eventIdentifier
        + '\''
        + ", location='"
        + location
        + '\''
        + ", deviceName='"
        + deviceName
        + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof Event)) return false;

    Event event = (Event) o;

    return new org.apache.commons.lang3.builder.EqualsBuilder()
        .append(getTime(), event.getTime())
        .append(getEventIdentifier(), event.getEventIdentifier())
        .append(getLocation(), event.getLocation())
        .append(getDeviceName(), event.getDeviceName())
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new org.apache.commons.lang3.builder.HashCodeBuilder(17, 37)
        .append(getTime())
        .append(getEventIdentifier())
        .append(getLocation())
        .append(getDeviceName())
        .toHashCode();
  }
}
