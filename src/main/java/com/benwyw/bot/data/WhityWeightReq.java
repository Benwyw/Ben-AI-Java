package com.benwyw.bot.data;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Valid
@Data
public class WhityWeightReq {

    @NotNull(message = "Page cannot be null")
    private int pageNumber;

    @NotNull(message = "Limit cannot be null")
    private int limit;

    @Schema(description = "Sort by fields", example = "['field1', 'field2']")
    private List<String> sortBy = Collections.emptyList();

    @Schema(description = "Sort direction for each field; true = descending", example = "['field1', 'field2']")

    private List<Boolean> sortDesc = Collections.emptyList();

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
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
