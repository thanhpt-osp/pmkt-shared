# UI Spec Template

> Copy file này thành `<feature>/ui-spec.md`. FE = `pmkt-web` (React + TypeScript per FE-1 base scope). UX framework defer FE-1 base.

## 1. Bối cảnh

<!-- Persona: <role A / role B> (theo security-baseline-rules.md §3)
     Workflow position: bước thứ N trong UC <link>
     Backend API: api-spec.md (same folder) -->

## 2. Screen/Component map

| ID | Tên màn hình / component | Trigger | Route |
|---|---|---|---|
| S1 | `<TenManHinh>List` | Navigation tab "<Resource>" | `/<service>/<resource>` |
| S2 | `<TenManHinh>Detail` | Click row ở S1 | `/<service>/<resource>/:id` |
| S3 | `<TenManHinh>Edit` | Click Edit button ở S2 | `/<service>/<resource>/:id/edit` |
| S4 | `<TenManHinh>Create` | Click "Tạo mới" ở S1 | `/<service>/<resource>/new` |

## 3. Wireframe (ASCII)

```
┌───────────────────────────────────────────────────────┐
│ <TenResource>                                  [+ Tạo]│
├───────────────────────────────────────────────────────┤
│ Search:  [_______________] [Lọc ▾]    Sort: [Newest ▾]│
├───────┬───────────────────────────┬────────────┬──────┤
│ Mã    │ Tên                       │ Trạng thái │ ⋯    │
├───────┼───────────────────────────┼────────────┼──────┤
│ X01   │ ...                       │ ✓ Active   │ [📝] │
│ X02   │ ...                       │ ✗ Inactive │ [📝] │
└───────┴───────────────────────────┴────────────┴──────┘
                   < 1 2 3 ... >
```

(Replace bằng Figma link / image embed khi UX design done)

## 4. Field validation

| Field | Required | Type | Validation | Error message |
|---|---|---|---|---|
| `<field1>` | ✅ | text 1-50 | Regex `^[A-Z0-9]+$` | "Mã chỉ chứa chữ in hoa và số" |
| `<field2>` | ✅ | dropdown | Phải chọn 1 trong enum | "Vui lòng chọn <loại>" |
| `<field3>` | ❌ | textarea 0-500 | Trim whitespace | — |

I18n key cho mọi validation message: `pmkt.<service>.<resource>.validation.<field>.<rule>`.

## 5. State + interaction

```
[Idle] ──Load─► [Loading]
   ▲                │
   │                ▼
   │           [Loaded]
   │           │       │
   │     ┌─────┘       └─────┐
   │     ▼                   ▼
   │  [Editing]          [Error]
   │     │                   │
   │     ▼                   │
   │  [Saving]               │
   │     │                   │
   ├─────┴───────────────────┘
   └──Cancel/Done──
```

Loading skeleton, error toast (RFC 9457 problem→toast UI mapper), optimistic update + rollback nếu API fail.

## 6. Component design (atomic level)

Atomic Design pattern:

- **Atom**: `<TextField>`, `<Select>`, `<Button>` (từ `pmkt-web/src/components/atoms/`)
- **Molecule**: `<FormField>`, `<SearchBar>`, `<Pagination>`
- **Organism**: `<<TenResource>Table>`, `<<TenResource>Form>`
- **Template**: `<<TenResource>ListPage>`, `<<TenResource>DetailPage>`

Re-use Material UI / Ant Design (chốt FE-1 base).

## 7. RBAC + visibility

| Role | Quyền |
|---|---|
| `pmkt-<svc>-reader` | View list + detail; KHÔNG thấy nút "Tạo/Sửa/Xoá" |
| `pmkt-<svc>-writer` | View + tạo + sửa; KHÔNG thấy nút "Xoá" |
| `pmkt-<svc>-admin` | Toàn quyền |
| `pmkt-ke-toan-truong` | Toàn quyền + duyệt action (vd "Ghi sổ") |

Verify ở FE qua claim JWT `realm_access.roles` (Keycloak); BE final enforce (Inv-4 + RBAC).

## 8. Accessibility (WCAG 2.1 AA)

- Form field có `<label for>` + `aria-describedby` cho error message.
- Tab order logical (top→bottom, left→right).
- Color contrast ≥ 4.5:1.
- Keyboard navigation Enter/Esc/Tab.
- Screen reader announce loading/error state.

## 9. i18n

Default `vi-VN`; FE-1 reserve cho `en-US` Phase 2.

Key naming: `pmkt.<service>.<resource>.<scope>.<key>`. Ví dụ:

```
pmkt.core.loaichungtu.list.title = "Danh sách Loại Chứng Từ"
pmkt.core.loaichungtu.list.action.create = "Tạo mới"
pmkt.core.loaichungtu.validation.ma.required = "Mã không được trống"
```

## 10. Test plan

- **Unit**: React Testing Library + Vitest, mọi component organism+.
- **E2E**: Playwright 1 happy path + 2 error path per màn hình.
- **A11y**: axe-core check 0 violation.
- **Visual regression**: Chromatic / Percy (Phase 2).
- **Coverage**: 70% line per FE-1 base scope.

## Liên quan

- BA UC: `<link>`
- Entity: `entity-spec.md`
- API: `api-spec.md`
- FE-1 base: [pmkt-mvp1-fe-base-scope.md (memory)] — wireframe + token cookie
- BDR ADR-I: cookie httpOnly via Kong BFF (security-baseline-rules.md §7 #10)
