package org.wso2.gw.emulator.core;

/**
 * Created by chamile on 12/7/15.
 */
public abstract class AbstractGivenBuilderContext<T extends AbstractConfigurationBuilderContext>{



    public abstract AbstractWhenBuilderContext given(T configurationContext);
}
