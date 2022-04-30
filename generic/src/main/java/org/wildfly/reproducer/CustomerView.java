package org.wildfly.reproducer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "customer")
public class CustomerView {
    private FormattedAddress address;
    private String name;
    private String phone;

    public CustomerView() {
    }

    @XmlElement
    public FormattedAddress getAddress() {
        return address;
    }

    @XmlElement
    public String getName() {
        return name;
    }

    @XmlElement
    public String getTelephone() {
        return phone;
    }

    public void setAddress(final FormattedAddress address) {
        this.address = address;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }
}
