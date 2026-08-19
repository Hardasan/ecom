import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-search',
  imports: [FormsModule, RouterLink],
  templateUrl: './search.html',
  styleUrl: './search.scss'
})
export class SearchPage {
  readonly a = ASSETS;
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  query = '';
  readonly hint = signal('نام محصول را جستجو کنید');

  submit() {
    const q = this.query.trim();
    if (!q) {
      this.hint.set('عبارت جستجو را وارد کنید');
      return;
    }
    void this.router.navigate(['/products'], { queryParams: { q, title: `جستجو: ${q}` } });
  }
}
