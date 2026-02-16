package com.ks.bayyinah.core.dto;

public class PageRequest {
  private int page;
  private int pageSize;

  public PageRequest(int page, int pageSize) {
    if (page < 1) {
      throw new IllegalArgumentException("Page number must be >= 1");
    }
    if (pageSize < 1) {
      throw new IllegalArgumentException("Page size must be >= 1");
    }
    this.page = page;
    this.pageSize = pageSize;
  }

  public int getPage() {
    return this.page;
  }

  public int getPageSize() {
    return this.pageSize;
  }

  public int getOffset() {
    return (this.page - 1) * this.pageSize;
  }

  public static PageRequest of(int page, int pageSize) {
    return new PageRequest(page, pageSize);
  }

  public static PageRequest first(int pageSize) {
    return new PageRequest(1, pageSize);
  }

  public PageRequest next() {
    return new PageRequest(this.page + 1, this.pageSize);
  }

  public PageRequest previous() {
    if (this.page <= 1) {
      throw new IllegalStateException("Already at the first page");
    }
    return new PageRequest(this.page - 1, this.pageSize);
  }

  @Override
  public String toString() {
    return "PageRequest{page=" + page + ", pageSize=" + pageSize + "}";
  }
}
