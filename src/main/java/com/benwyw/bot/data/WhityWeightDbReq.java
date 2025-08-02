package com.benwyw.bot.data;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WhityWeightDbReq {
    private Page<WhityWeight> page;
    private List<String> sortBy = new ArrayList<>();
    private List<Boolean> sortDesc = new ArrayList<>();

    public Page<WhityWeight> getPage() {
        return page;
    }

    public void setPage(Page<WhityWeight> page) {
        this.page = page;
    }

    public List<String> getSortBy() {
        return sortBy;
    }

    public void setSortBy(List<String> sortBy) {
        this.sortBy = sortBy;
    }

    public List<Boolean> getSortDesc() {
        return sortDesc;
    }

    public void setSortDesc(List<Boolean> sortDesc) {
        this.sortDesc = sortDesc;
    }
}
