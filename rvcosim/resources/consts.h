#pragma once

#include <cstdint>

#include "glog.h"

/* write this csr with non-zero value to end simulation. */
constexpr uint32_t CSR_MSIMEND = 0x7cc;

/* sync with spike. */
enum RegClass { GPR = 0, FPR = 1, VRF = 2, CSR = 4 };

inline RegClass to_reg_class(bool fp, bool vector) { return (RegClass)(fp + (vector << 1)); }

inline const char *reg_class_name(RegClass rc) {
  switch (rc) {
  case GPR:
    return "gpr";
  case FPR:
    return "fpr";
  case VRF:
    return "vrf";
  case CSR:
    return "csr";
  default:
    CHECK_S(false) << "unreachable";
  }
}
