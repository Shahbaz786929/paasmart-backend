package com.paasmart.backend.common;

import org.springframework.http.ResponseEntity;

import java.util.List;

public final class PageUtil {
    private PageUtil() {}

    public static final int DEFAULT_CAP = 300;   // page/size na diya to itne se zyada nahi bhejenge
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static <T> ResponseEntity<List<T>> slice(List<T> all, Integer page, Integer size) {
        int total = all.size();

        if (page == null && size == null) {
            List<T> capped = total > DEFAULT_CAP ? all.subList(0, DEFAULT_CAP) : all;
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(total))
                    .body(capped);
        }

        int s = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int p = (page == null || page < 0) ? 0 : page;
        long from = (long) p * s;
        List<T> part = from >= total
                ? List.of()
                : all.subList((int) from, (int) Math.min(from + s, total));

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(total))
                .header("X-Page", String.valueOf(p))
                .header("X-Size", String.valueOf(s))
                .body(part);
    }
}