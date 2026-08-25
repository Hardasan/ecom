import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminStatsService } from '../services/admin-stats.service';
import { AdminOrderService } from '../services/admin-order.service';
import { AdminStatsDto, OrderDto } from '../../core/models';
import { formatFaDate, formatPrice, orderStatusLabel } from '../../core/format';
import { orderStatusTone } from '../admin-format';

@Component({
  selector: 'app-admin-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
  private readonly statsApi = inject(AdminStatsService);
  private readonly ordersApi = inject(AdminOrderService);

  readonly stats = signal<AdminStatsDto | null>(null);
  readonly recent = signal<OrderDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  readonly money = formatPrice;
  readonly date = formatFaDate;
  readonly statusLabel = orderStatusLabel;
  readonly tone = orderStatusTone;

  ngOnInit(): void {
    this.statsApi.stats().subscribe({
      next: (s) => {
        this.stats.set(s);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('آمار داشبورد خوانده نشد');
        this.loading.set(false);
      }
    });
    this.ordersApi.list().subscribe({
      next: (list) => this.recent.set((list ?? []).slice(0, 6)),
      error: () => undefined
    });
  }
}
