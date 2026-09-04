import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { OrderService } from '../../core/order.service';
import { ReturnService, RETURN_REASONS, returnReasonLabel } from '../../core/return.service';
import { OrderDto, OrderItemDto, ReturnReason, ReturnRequestDto } from '../../core/models';
import { formatFaDate, formatPrice, imageSrc, toFa, toNumber, variantLabel } from '../../core/format';
import { FaNumPipe } from '../../core/fa-num.pipe';

type Step = 'select' | 'confirm' | 'success';

interface Selection {
  selected: boolean;
  reason: ReturnReason | null;
}

/**
 * Returns — steps 2-4 for one order (screens «انتخاب کالا برای مرجوعی» → «تأیید درخواست مرجوعی» →
 * success). Kept as one component with an internal step signal so the picked items + reasons survive
 * between steps without a shared store; on submit it POSTs the request and shows the receipt.
 */
@Component({
  selector: 'app-return-flow',
  imports: [RouterLink, FormsModule, FaNumPipe],
  templateUrl: './return-flow.html',
  styleUrl: './returns.scss'
})
export class ReturnFlow implements OnInit {
  readonly a = ASSETS;
  readonly reasons = RETURN_REASONS;
  readonly reasonLabel = returnReasonLabel;

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly orderApi = inject(OrderService);
  private readonly returnsApi = inject(ReturnService);
  private readonly auth = inject(AuthService);

  readonly step = signal<Step>('select');
  readonly order = signal<OrderDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly submitting = signal(false);
  readonly created = signal<ReturnRequestDto | null>(null);

  // orderItemId → selection state (whether it's being returned + its reason).
  readonly selections = signal<Record<number, Selection>>({});
  iban = '';
  note = '';

  readonly items = computed<OrderItemDto[]>(() => this.order()?.items ?? []);
  readonly selectedItems = computed(() =>
    this.items().filter((it) => this.selections()[it.id]?.selected && this.selections()[it.id]?.reason)
  );
  readonly allSelected = computed(
    () => this.items().length > 0 && this.items().every((it) => this.selections()[it.id]?.selected)
  );
  readonly refundTotal = computed(() =>
    this.selectedItems().reduce((sum, it) => sum + toNumber(it.lineTotal), 0)
  );
  readonly canContinue = computed(() => this.selectedItems().length > 0);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('orderId'));
    this.orderApi.get(id).subscribe({
      next: (o) => {
        this.order.set(o);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('سفارش پیدا نشد یا قابل مرجوعی نیست.');
        this.loading.set(false);
      }
    });
    this.auth.getIban().subscribe({ next: (r) => (this.iban = r.iban ?? ''), error: () => undefined });
  }

  // ---- display helpers -------------------------------------------------------------------------
  orderCode(): string {
    return toFa(this.order()?.id ?? '');
  }

  itemImage(it: OrderItemDto): string {
    return imageSrc(it.mainImage);
  }

  itemVariant(it: OrderItemDto): string {
    return variantLabel(it.variantType, it.variantValue);
  }

  linePrice(it: OrderItemDto): string {
    return formatPrice(it.lineTotal);
  }

  refundText(): string {
    return formatPrice(this.refundTotal());
  }

  createdDate(): string {
    return formatFaDate(this.created()?.createdAt);
  }

  // ---- selection -------------------------------------------------------------------------------
  isSelected(it: OrderItemDto): boolean {
    return !!this.selections()[it.id]?.selected;
  }

  reasonOf(it: OrderItemDto): ReturnReason | null {
    return this.selections()[it.id]?.reason ?? null;
  }

  toggle(it: OrderItemDto): void {
    const map = { ...this.selections() };
    const current = map[it.id] ?? { selected: false, reason: null };
    map[it.id] = { ...current, selected: !current.selected };
    this.selections.set(map);
  }

  setReason(it: OrderItemDto, reason: ReturnReason): void {
    const map = { ...this.selections() };
    map[it.id] = { selected: true, reason };
    this.selections.set(map);
  }

  toggleAll(): void {
    const on = !this.allSelected();
    const map: Record<number, Selection> = {};
    for (const it of this.items()) {
      map[it.id] = { selected: on, reason: this.selections()[it.id]?.reason ?? null };
    }
    this.selections.set(map);
  }

  // ---- steps -----------------------------------------------------------------------------------
  goConfirm(): void {
    if (this.canContinue()) this.step.set('confirm');
  }

  back(): void {
    this.step.set(this.step() === 'confirm' ? 'select' : 'select');
  }

  submit(): void {
    const o = this.order();
    if (!o || this.submitting()) return;
    this.submitting.set(true);
    this.error.set('');
    this.returnsApi
      .create({
        orderId: o.id,
        note: this.note.trim() || null,
        iban: this.iban.trim() || null,
        items: this.selectedItems().map((it) => ({
          orderItemId: it.id,
          quantity: it.quantity,
          reason: this.selections()[it.id]!.reason!
        }))
      })
      .subscribe({
        next: (res) => {
          this.created.set(res);
          this.submitting.set(false);
          this.step.set('success');
        },
        error: (err) => {
          this.submitting.set(false);
          this.error.set(err?.error?.message ?? 'ثبت درخواست مرجوعی ناموفق بود. لطفاً دوباره تلاش کنید.');
        }
      });
  }
}
