package com.ks.bayyinah.core.dto;

import java.util.List;

public class Page<T> {
  private List<T> content;
  private final int page;
  private final int pageSize;
  private final long totalElements;
  private final int totalPages;

  public Page(List<T> content, int page, int pageSize, int totalElements) {
    this.content = content;
    this.page = page;
    this.pageSize = pageSize;
    this.totalElements = totalElements;
    this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
  }

  public List<T> getContent() {
    return content;
  }

  public int getPage() {
    return page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public long getTotalElements() {
    return totalElements;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public int getNumberOfElements() {
    return content.size();
  }

  public boolean hasNext() {
    return page < totalPages;
  }

  public boolean hasPrevious() {
    return page > 1;
  }

  public boolean isFirst() {
    return page == 1;
  }

  public boolean isLast() {
    return page >= totalPages;
  }

  public boolean isEmpty() {
    return content.isEmpty();
  }

  public static <T> Page<T> empty() {
    return new Page<>(List.of(), 1, 0, 0);
  }

  @Override
  public String toString() {
    return "Page{page=" + page + "/" + totalPages + ", size=" + content.size() + "/" + totalElements + "}";
  }
}
