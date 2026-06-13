package com.note.ai.tools;

public interface BaseTool {


    /**
     * 工具英文名称
     * @return 返回工具英文名称
     */
    String getToolName();

    /**
     * 工具显示名称
     * @return 返回工具显示名称
     */
    String getToolDisplayName();

}
