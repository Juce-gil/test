/**
 * 后端返回的图片地址多为相对路径或带后端域名的完整 URL，
 * 需转成当前域名下的完整 URL 才能在前端显示（走代理、避免跨域/鉴权问题）。
 */
const IMAGE_BASE = "/api/campus-product-sys/v1.0";

export function toFullImageUrl(url) {
  if (!url || typeof url !== "string") return "";
  const trimmed = url.trim();
  if (!trimmed) return "";
  const base = IMAGE_BASE.replace(/\/$/, "");
  const origin = window.location.origin;
  if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
    try {
      const u = new URL(trimmed);
      const pathname = u.pathname || "";
      if (pathname.indexOf(base) !== -1) {
        return origin + pathname + (u.search || "");
      }
      return trimmed;
    } catch (_) {
      return trimmed;
    }
  }
  const path = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
  return origin + base + path;
}
