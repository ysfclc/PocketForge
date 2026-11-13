#ifndef POCKETFORGE_BRIDGE_CONTRACT_H
#define POCKETFORGE_BRIDGE_CONTRACT_H
#include <stdint.h>
#ifdef __cplusplus
extern "C" {
#endif
uint32_t pocketforge_abi_version(void);
int32_t pocketforge_health_check(void);
#ifdef __cplusplus
}
#endif
#endif
