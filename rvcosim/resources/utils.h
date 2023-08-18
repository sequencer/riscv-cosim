#include <fmt/core.h>
#include <fmt/format.h>

/* FIXME: get rid of env, use dpi instead. */
inline char *env(const char *name) {
  char *val = std::getenv(name);
  CHECK_S(val != nullptr) << fmt::format(
      "cannot find environment variable '{}'", name);
  return val;
}
