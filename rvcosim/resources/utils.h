#include <fmt/core.h>
#include <fmt/format.h>

/* FIXME: get rid of env, use dpi instead. */
inline char *env(const char *name) {
  char *val = std::getenv(name);
  ASSERT(val != nullptr) << fmt::format("cannot find environment variable '{}'", name);
  return val;
}

inline uint32_t expand_mask(uint8_t mask) {
  uint64_t x = mask & 0xF;
  x = (x | (x << 14)) & 0x00030003;
  x = (x | (x << 7)) & 0x01010101;
  x = (x << 8) - x;
  return (uint32_t)x;
}
