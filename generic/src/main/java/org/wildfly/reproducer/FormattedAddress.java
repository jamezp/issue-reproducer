package org.wildfly.reproducer;

import java.util.Collection;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "address")
public class FormattedAddress {

    private Collection<String> address;

    public void setAddress(Collection<String> address) {
        this.address = address;
    }

    @XmlElement(name = "line")
    public Collection<String> getAddress() {
        return address;
    }
}
