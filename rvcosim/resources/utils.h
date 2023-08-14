#include <fmt/core.h>
#include <fmt/format.h>

inline char *env(const char *name) {
  char *val = std::getenv(name);
  CHECK_S(val != nullptr) << fmt::format(
      "cannot find environment variable '{}'", name);
  return val;
}
