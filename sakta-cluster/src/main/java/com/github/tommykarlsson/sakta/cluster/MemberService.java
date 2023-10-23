package com.github.tommykarlsson.sakta.cluster;

import java.util.function.Consumer;

public class MemberService {

    Member getOwnerOfAddress(Object address) {
        return new Member();
    }

    <T> void tellOnOwner(Object address, Consumer<T> teller) {

    }
}
