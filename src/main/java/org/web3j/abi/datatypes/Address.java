package org.web3j.abi.datatypes;

import java.math.BigInteger;

import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

/**
 * Address type, which is equivalent to uint160.
 */
public class Address implements Type<String> {

    public static final String TYPE_NAME = "address";
    public static final int LENGTH = 64;
    public static final int LENGTH_IN_HEX = 42;
    public static final Address DEFAULT = new Address(BigInteger.ZERO);

    private final Uint256 value;

    public Address(Uint256 value) {
        this.value = value;
    }

    public Address(BigInteger value) {
        this(new Uint256(value));
    }

    public Address(String hexValue) {
        this(Numeric.toBigInt(hexValue));
    }

    public Uint256 toUint256() {
        return value;
    }

    @Override
    public String getTypeAsString() {
        return TYPE_NAME;
    }

    @Override
    public String toString() {
        return Numeric.toHexStringWithPrefixZeroPadded(
                value.getValue(), LENGTH_IN_HEX);
    }

    @Override
    public String getValue() {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Address address = (Address) o;

        return value != null ? value.equals(address.value) : address.value == null;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
