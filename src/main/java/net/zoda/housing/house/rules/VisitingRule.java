package net.zoda.housing.house.rules;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum VisitingRule {

    PUBLIC(true),
    PRIVATE(true)
    ;

    public final boolean single;

}
