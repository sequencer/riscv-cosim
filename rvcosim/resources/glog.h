#pragma once

#include <fmt/core.h>
#include <glog/logging.h>

namespace google {

class CheckFailedException : public std::runtime_error {
public:
  explicit CheckFailedException() : std::runtime_error("check failed") {}
};

class LogMessageFatal_S : public LogMessage {
public:
  LogMessageFatal_S(const char *file, int line)
      : LogMessage(file, line, GLOG_ERROR){};
  LogMessageFatal_S(const char *file, int line, const CheckOpString &result)
      : LogMessage(file, line, GLOG_ERROR) {
    stream() << "Check failed: " << (*result.str_) << " ";
  };
  [[noreturn]] ~LogMessageFatal_S() noexcept(false) {
    Flush();
    throw CheckFailedException();
  };
};

} // namespace google

#define CHECK_S(condition)                                                     \
  LOG_IF(FATAL_S, GOOGLE_PREDICT_BRANCH_NOT_TAKEN(!(condition)))               \
      << "Check failed: " #condition " "

#define COMPACT_GOOGLE_LOG_FATAL_S google::LogMessageFatal_S(__FILE__, __LINE__)
